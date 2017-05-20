import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.recommendation.ALS
import org.apache.spark.mllib.recommendation.MatrixFactorizationModel
import org.apache.spark.mllib.recommendation.Rating

object MainObject {

  val conf = new SparkConf()
    .setMaster("local[4]")
    .setAppName("RecomenderComparison")
  val sc = new SparkContext(conf)

  def main(args: Array[String]) {
    val users = sc.textFile("ml-100k/u.user").map(u => u.trim.split("\\|")).cache()
    val genres = sc.textFile("ml-100k/u.genre").map(u => u.trim.split("\\|")).cache()
    val items = sc.textFile("ml-100k/u.item").map(u => u.trim.replace("||", "|").split("\\|")).cache()
    val occupations = sc.textFile("ml-100k/u.occupation").cache()
    val ratings = sc.textFile("ml-100k/u.data").map(_.split("\t")  match { case Array(user, item, rate, timestamp) =>
      Rating(user.toInt, item.toInt, rate.toDouble)
    }).cache()



    // Build the recommendation model using ALS
    val rank = 10          // 10 - 20
    val numIterations = 10 // 50 - 100
    val model = ALS.train(ratings, rank, numIterations, 0.01) // pollaplasia 3

    // Evaluate the model on rating data
    val usersProducts = ratings.map { case Rating(user, product, rate) =>
      (user, product)
    }
    val predictions =
      model.predict(usersProducts).map { case Rating(user, product, rate) =>
        ((user, product), rate)
      }

    val ratesAndPreds = ratings.map { case Rating(user, product, rate) =>
      ((user, product), rate)
    }.join(predictions)

    val MSE = ratesAndPreds.map { case ((user, product), (r1, r2)) =>
      val err = (r1 - r2)
      err * err
    }.mean()
    println("Mean Squared Error = " + MSE)

    // Save and load model
    model.save(sc, "target/tmp/myCollaborativeFilter")
    val sameModel = MatrixFactorizationModel.load(sc, "target/tmp/myCollaborativeFilter")
    /////
    //
    //
    //    collaborative filtering
    //
    //


  }
}