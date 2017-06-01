
import java.util

import breeze.linalg.{Axis, DenseMatrix, pinv}
import breeze.optimize.linear.PowerMethod.BDM
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.Matrix
import org.apache.spark.mllib.linalg.distributed.{CoordinateMatrix, MatrixEntry}
import org.apache.spark.mllib.recommendation.Rating
import org.apache.spark.rdd.RDD




object ContentBased {

  val sparkContext: SparkContext = Infrastructure.sparkContext

  def main(args: Array[String]) {

    val normalizationFactor: Double = 0.9

//    val trainingSet = Infrastructure.dataSetList(3)._1
//
//    val testingSet = Infrastructure.dataSetList(3)._2
//
//
//    println(getMetricsForDataset(normalizationFactor, trainingSet, testingSet))

    val bestNormalizationFactor = Infrastructure.normalizationFactorsList.map { v =>
      val sum = Infrastructure.dataSetList.map(dataSet => getMetricsForDataset(v, dataSet._1, dataSet._2)).map(u => u._5).sum
      val mean = sum/Infrastructure.dataSetList.size
      (v, mean)
    }.maxBy(v=> v._2)

    println(bestNormalizationFactor)

//    Infrastructure.dataSetList
//      .map(dataSet => getMetricsForDataset(normalizationFactor, dataSet._1, dataSet._2))
//      .foreach(metric => println(metric))
//    println("training set", "testing set", "MSE", "RMSE", "MAE", "Execution Time")

  }


  private def getMetricsForDataset(normalizationFactor: Double, trainingSet: String, testingSet: String) = {
    val startingTime = System.currentTimeMillis()

    val itemsMatrixEntries: RDD[MatrixEntry] = generateItemMatrixEntries

    val itemMatrix: Matrix = new CoordinateMatrix(itemsMatrixEntries).toBlockMatrix().toLocalMatrix()
    val itemMatrixBreeze = toBreeze(itemMatrix).copy

    val ratings = sparkContext.textFile(trainingSet)
      .map(_.split("\t") match {
        case Array(user, item, rate, timestamp) => Rating(user.toInt, item.toInt, rate.toDouble)
      }).cache()

    val usersRatings = ratings.groupBy(r => r.user)
      .map(v => (v._1, generateUserMatrix(v._2)))

    val refinedMatrices = usersRatings
      .map(v => (v._1, getRefinedMatrices(v._2, itemMatrixBreeze)))

    val userWeights = refinedMatrices.map(v => Pair(v._1, generateWeight(v, normalizationFactor)))
    val testRatings = sparkContext.textFile(testingSet)
      .map(_.split("\t") match {
        case Array(user, item, rate, timestamp) => Rating(user.toInt, item.toInt, rate.toDouble)
      }).cache()

    // remove rating from dataset
    val usersProducts = testRatings.map {
      case Rating(user, product, rate) => (user, product)
    }

    // predict
    val b = sparkContext.broadcast(userWeights.collect())

    val predictions = usersProducts.map(v =>
      ((v._1, v._2),
        predict(
          b.value.apply(v._1 - 1)._2,
          getRow(itemMatrixBreeze, v._2 - 1))
      ))

    val ratesAndPredictions = testRatings.map {
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

    (trainingSet, testingSet, MSE, RMSE, MAE, executionTime, normalizationFactor)
  }

  private def predict(weight: DenseMatrix[Double], item: DenseMatrix[Double]): Double = {
    val result = item.t * weight
    if (result.data.length > 1) {
      println("something went wrong on prediction")
      0
    }
    else result.data.apply(0)

  }

  private def generateWeight(v: (Int, (DenseMatrix[Double], DenseMatrix[Double])), normalizationFactor: Double): DenseMatrix[Double] = {
    calculateWeightsWithNormalizationFactor(v._2._2, v._2._1, normalizationFactor)
  }

  private def calculateWeightsWithNormalizationFactor(ratingMatrix :DenseMatrix[Double], itemMatrix: DenseMatrix[Double], normalizationFactor: Double): DenseMatrix[Double] = {
    //pinv( ratingMatrix) * itemMatrix // (lI + RTR)^-1 RTM R= ratingMatrix, M = movie Matrix
    val lambdaIdentity = DenseMatrix.eye[Double](ratingMatrix.cols) :* normalizationFactor
    pinv(
      lambdaIdentity
        +
        (ratingMatrix.t * ratingMatrix)
    ) * (ratingMatrix.t * itemMatrix)
  }


  private def calculateWeightsWithoutNormalizationFactor(ratingMatrix :DenseMatrix[Double], itemsMatrix: DenseMatrix[Double]): DenseMatrix[Double] = {
    pinv(ratingMatrix) * itemsMatrix
  }

  def getRefinedMatrices(userMatrix: DenseMatrix[Double], itemMatrix:DenseMatrix[Double]): (DenseMatrix[Double], DenseMatrix[Double]) = {
    var sequence = Seq[Int]()
    userMatrix.foreachKey { v =>
      if (userMatrix(v._1,v._2) == 0) {
        sequence = sequence :+ v._1
      }
    }
    val localItemMatrix = itemMatrix.delete(sequence, Axis._0)   //// here doesnt delete rows
    val localUserMatrix = userMatrix.delete(sequence, Axis._0)
    (localUserMatrix, localItemMatrix)
  }


  def getRow(matrix: DenseMatrix[Double], row: Int): DenseMatrix[Double] = {
    val numberOfColumns = matrix.cols
    val array = new Array[Double](numberOfColumns)
    for (i <- 0 until numberOfColumns){
      array(i)=matrix(row,i)
    }
    new DenseMatrix(numberOfColumns ,1, array)
  }

  def generateUserMatrix(userRatings: Iterable[Rating]): DenseMatrix[Double] = {

    val numberOfItems = Infrastructure.items.count().toInt
    val array = new Array[Double](numberOfItems)
    util.Arrays.fill(array, 0)
    userRatings.foreach(r => array(r.product - 1) = r.rating)
    new DenseMatrix(numberOfItems ,1, array)
  }

  private def toBreeze(matrix: Matrix): DenseMatrix[Double] = {
    val breezeMatrix = new BDM(matrix.numRows, matrix.numCols, matrix.toArray)
    if (!matrix.isTransposed) {
      breezeMatrix
    } else {
      breezeMatrix.t
    }
  }

  private def generateItemMatrixEntries: RDD[MatrixEntry] = {
    Infrastructure.items.flatMap(a => Array(
    MatrixEntry(a(0).toLong - 1, 0, a(4).toInt),
    MatrixEntry(a(0).toLong - 1, 1, a(5).toInt),
    MatrixEntry(a(0).toLong - 1, 2, a(6).toInt),
    MatrixEntry(a(0).toLong - 1, 3, a(7).toInt),
    MatrixEntry(a(0).toLong - 1, 4, a(8).toInt),
    MatrixEntry(a(0).toLong - 1, 5, a(9).toInt),
    MatrixEntry(a(0).toLong - 1, 6, a(10).toInt),
    MatrixEntry(a(0).toLong - 1, 7, a(11).toInt),
    MatrixEntry(a(0).toLong - 1, 8, a(12).toInt),
    MatrixEntry(a(0).toLong - 1, 9, a(13).toInt),
    MatrixEntry(a(0).toLong - 1, 10, a(14).toInt),
    MatrixEntry(a(0).toLong - 1, 11, a(15).toInt),
    MatrixEntry(a(0).toLong - 1, 12, a(16).toInt),
    MatrixEntry(a(0).toLong - 1, 13, a(17).toInt),
    MatrixEntry(a(0).toLong - 1, 14, a(18).toInt),
    MatrixEntry(a(0).toLong - 1, 15, a(19).toInt),
    MatrixEntry(a(0).toLong - 1, 16, a(20).toInt),
    MatrixEntry(a(0).toLong - 1, 17, a(21).toInt),
    MatrixEntry(a(0).toLong - 1, 18, a(22).toInt))
    )
  }
}
