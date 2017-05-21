import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.recommendation.ALS
import org.apache.spark.mllib.recommendation.Rating

object LatentFactors {

  val sparkConfiguration = new SparkConf()
    .setMaster("local[4]")
    .setAppName("RecomenderComparison")
  val sparkContext = {
    val sc = new SparkContext(sparkConfiguration)
    sc.setCheckpointDir("checkpoint/") // set checkpoint dir to avoid stack overflow
    sc
  }


  def main(args: Array[String]) {

    //import data to rdd
    val users = sparkContext.textFile("ml-100k/u.user").map(u => u.trim.split("\\|")).cache()
    val genres = sparkContext.textFile("ml-100k/u.genre").map(u => u.trim.split("\\|")).cache()
    val items = sparkContext.textFile("ml-100k/u.item").map(u => u.trim.replace("||", "|").split("\\|")).cache()
    val occupations = sparkContext.textFile("ml-100k/u.occupation").cache()
    val ratings = sparkContext.textFile("ml-100k/u5.base").map(_.split("\t") match { case Array(user, item, rate, timestamp) =>
      Rating(user.toInt, item.toInt, rate.toDouble)
    }).cache()

    //// Build the recommendation model using ALS
    val rank = 15 // 10 - 20
    val numIterations = 90 // 50 - 100
    val model = ALS.train(ratings, rank, numIterations, 0.09) // pollaplasia 3


    //// compare algorithms on prediction, user recommendation, product recommendation
    //import test dataset
    val testRatings = sparkContext.textFile("ml-100k/u5.test").map(_.split("\t") match { case Array(user, item, rate, timestamp) =>
      Rating(user.toInt, item.toInt, rate.toDouble)
    }).cache()

    // remove rating from dataset
    val usersProducts = testRatings.map {
      case Rating(user, product, rate) => (user, product)
    }

    // predict the rating
    val predictions = model.predict(usersProducts).map {
      case Rating(user, product, rate) => ((user, product), rate)
    }

    // join rdd to get the rating and the prediction value for each combination
    val ratesAndPreds = testRatings.map {
      case Rating(user, product, rate) => ((user, product), rate)
    }.join(predictions)

    // calculate MSE (Mean Square Error)
    val MSE = ratesAndPreds.map { case ((user, product), (r1, r2)) =>
      val err = r1 - r2
      err * err
    }.mean()

    // calculate RMSE (Root Mean Square Error)
    val RMSE = Math.sqrt(MSE)

    // calculate MAE (Mean Absolute Error)
    val MAE = ratesAndPreds.map { case ((user, product), (r1, r2)) =>
      val err = r1 - r2
      Math.abs(err)
    }.mean()


    println(
      "Mean Squared Error = " + MSE + "\n" +
      "Root Mean Squared Error = " + RMSE + "\n" +
      "Mean Absolute Error = " + MAE
    )

//    model.recommendProducts(858, 10).foreach(u => println(u.product))
  }
}