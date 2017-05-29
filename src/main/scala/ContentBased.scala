
import java.util

import breeze.linalg.{*, Axis, DenseMatrix, SliceMatrix, inv, pinv}
import breeze.numerics._
import breeze.optimize.linear.PowerMethod.BDM
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.{Matrices, Matrix}
import org.apache.spark.mllib.linalg.distributed.{CoordinateMatrix, IndexedRowMatrix, MatrixEntry, RowMatrix}
import org.apache.spark.mllib.recommendation.Rating
import org.apache.spark.rdd.RDD




object ContentBased {

  val sparkContext: SparkContext = Infrastructure.sparkContext
  val normalizationFactor: Double = 0.09


  def main(args: Array[String]) {
    //    Infrastructure.dataSetList
    //      .map(dataSet => getMetricsForDataset(dataSet._1, dataSet._2))
    //      .foreach(metric => println(metric))
    //    println("training set", "testing set", "MSE", "RMSE", "MAE", "Execution Time")

    val itemsMatrixEntries: RDD[MatrixEntry] = generateItemMatrixEntries
    val itemMatrix: Matrix = new CoordinateMatrix(itemsMatrixEntries).toBlockMatrix().toLocalMatrix()

    val itemMatrixBreeze = toBreeze(itemMatrix).copy

    val ratings = sparkContext.textFile("ml-100k/u1.base")
      .map(_.split("\t") match {
        case Array(user, item, rate, timestamp) => Rating(user.toInt, item.toInt, rate.toDouble)
    }).cache()

    val usersRatings = ratings.groupBy(r => r.user)
      .map(v => (v._1, generateUserMatrix(v._2)))

    val refinedMatrices = usersRatings
      .map(v => (v._1, getRefinedMatrices(v._2, itemMatrixBreeze)))

    val userWeights = refinedMatrices.map(v => Pair(v._1, generateWeight(v)))

    val testRatings = sparkContext.textFile("ml-100k/u1.test")
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
          b.value.apply(v._1)._2,
           getRow(itemMatrixBreeze, v._2 - 1))
      ))

    val ratesAndPredictions = testRatings.map {
      case Rating(user, product, rate) => ((user, product), rate)
    }.join(predictions)

    val MSE = Metrics.getMSE(ratesAndPredictions)


    println(MSE)

//
//    val temp = usersRatings.first()
//    val tempUser: Int = 770
//    val tempMatrix = temp._2
//
//    val refined: (DenseMatrix[Double], DenseMatrix[Double]) = refinedMatrices.filter(v => v._1==tempUser).map(v=> v._2).first()
//
////    println(refined._1.data.deep.mkString(","))
////    println(refined._2.data.deep.mkString(","))
//
//    val weight = userWeights.filter(v => v._1 == tempUser).map(v => v._2).first()
//
////    println(weight.toArray.deep.mkString(","))
////    println(weight.cols, weight.rows)
//
//    val row: DenseMatrix[Double] = getRow(itemMatrix,13)
//
//
//    val prediction = predict(weight, row)
//    val array = new Array[Double](19)
//    val allOne = util.Arrays.fill(array, 1)
//    val total = predict(weight, new DenseMatrix[Double](19, 1, array)) /// 0.49
//
//    val x: DenseMatrix[Double] = row * tempMatrix.t
//    println(refined._1.data.deep.mkString(","))
//    println(refined._2.data.deep.mkString(","))
//    println(weight.data.deep.mkString(","))
//    println(row.data.deep.mkString(","))
//    println(prediction.data.deep.mkString(","))
//    println(total.data.deep.mkString(","))
//    println(row.cols, row.rows)
//


  }


  private def getUsersWeight(userWeights: RDD[(Int, DenseMatrix[Double])], user: Int) = {
    userWeights
  }

  private def predict(weight: DenseMatrix[Double], item: DenseMatrix[Double]): Double = {
    val result = item.t * weight
    if (result.data.length > 1) {
      println("something went wrong on prediction")
      0
    }
    else result.data.apply(0)

  }

  private def generateWeight(v: (Int, (DenseMatrix[Double], DenseMatrix[Double]))) = {
    calculateWeightsWithoutNormalizationFactor(v._2._2, v._2._1)
  }

  private def calculateWeightsWithNormalizationFactor(ratingMatrix :DenseMatrix[Double], itemMatrix: DenseMatrix[Double]) = {
    pinv(ratingMatrix) * itemMatrix // (lI + RTR)^-1 RTM R= ratingMatrix, M = movie Matrix
  }

  private def calculateWeightsWithoutNormalizationFactor(ratingMatrix :DenseMatrix[Double], itemsMatrix: DenseMatrix[Double]) = {
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

  private def toBreeze(matrix: Matrix)= {
    val breezeMatrix = new BDM(matrix.numRows, matrix.numCols, matrix.toArray)
    if (!matrix.isTransposed) {
      breezeMatrix
    } else {
      breezeMatrix.t
    }
  }

  private def generateItemMatrixEntries = {
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
