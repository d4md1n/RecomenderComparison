import org.apache.spark.SparkContext
import org.apache.spark.mllib.recommendation.{ALS, Rating}
import org.apache.spark.rdd.RDD

object LatentFactors {

  val sparkContext: SparkContext = Infrastructure.sparkContext

  def main(args: Array[String]) {
//    Infrastructure.dataSetList
//      .map(dataSet => getMetricsForDataset(dataSet._1, dataSet._2))
//      .foreach(metric => println(metric))
//    println("training set", "testing set", "MSE", "RMSE", "MAE", "Execution Time")

    val bestNormalizationFactor = Infrastructure.normalizationFactorsList.map { v =>
      val sum = Infrastructure.dataSetList.map(dataSet => getMetricsForDataset(dataSet._1, dataSet._2, v)).map(u => u._5).sum
      val mean = sum/Infrastructure.dataSetList.size
      (v, mean)
    }.maxBy(v=> v._2)

    println(bestNormalizationFactor)
  }

  private def getMetricsForDataset(trainingSet:String, testingSet:String, normalizationFactor: Double) = {

    val startingTime = System.currentTimeMillis()

    val ratings = sparkContext.textFile(trainingSet).map(_.split("\t") match { case Array(user, item, rate, timestamp) =>
      Rating(user.toInt, item.toInt, rate.toDouble)
    }).cache()

    //// Build the recommendation model using ALS
    val rank = 15 // 10 - 20
    val numIterations = 75 // 50 - 100
    val model = ALS.train(ratings, rank, numIterations, normalizationFactor) // pollaplasia 3


    //// compare algorithms on prediction, user recommendation, product recommendation
    //import test dataset
    val testRatings = sparkContext.textFile(testingSet).map(_.split("\t") match { case Array(user, item, rate, timestamp) =>
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
    val ratesAndPredictions: RDD[((Int, Int), (Double, Double))] = testRatings.map {
      case Rating(user, product, rate) => ((user, product), rate)
    }.join(predictions)

    ///// Metrics ////

    // calculate MSE (Mean Square Error)
    val MSE = Metrics.getMSE(ratesAndPredictions)

    // calculate RMSE (Root Mean Square Error)
    val RMSE = Math.sqrt(MSE)

    // calculate MAE (Mean Absolute Error)
    val MAE = Metrics.getMAE(ratesAndPredictions)

    val endingTime = System.currentTimeMillis()

    val executionTime = endingTime - startingTime

    (trainingSet, testingSet, MSE, RMSE, MAE, executionTime)
  }
}