import LatentFactors.sparkContext
import ml.dmlc.xgboost4j.scala.spark.XGBoost
import org.apache.spark.mllib.recommendation.Rating

object XGBoostImplementation {
  def main(args: Array[String]) {
    Infrastructure.dataSetList
     // .map(dataSet => getMetricsForDataset(dataSet._1, dataSet._2))
      .foreach(metric => println(metric))
    println("training set", "testing set", "MSE", "RMSE", "MAE", "Execution Time")
  }

//  private def getMetricsForDataset(trainingSet:String, testingSet:String) = {
//
//    val ratings = sparkContext.textFile(trainingSet).map(_.split("\t") match { case Array(user, item, rate, timestamp) =>
//      Rating(user.toInt, item.toInt, rate.toDouble)
//    }).cache()
//
//
//    val paramMap = List(
//      "eta" -> 0.1f,
//      "max_depth" -> 2,
//      "objective" -> "binary:logistic").toMap
//
//    val model = XGBoost.train(ratings, paramMap, numRound = 2, nWorkers = 5, useExternalMemory = true)
//
//
//    (trainingSet, testingSet, MSE, RMSE, MAE, executionTime)
//  }
}
