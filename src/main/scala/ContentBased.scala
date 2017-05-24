
import java.util

import breeze.linalg.{Axis, DenseMatrix, SliceMatrix}
import breeze.numerics._
import breeze.optimize.linear.PowerMethod.BDM
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.{Matrices, Matrix}
import org.apache.spark.mllib.linalg.distributed.{CoordinateMatrix, IndexedRowMatrix, MatrixEntry, RowMatrix}
import org.apache.spark.mllib.recommendation.Rating
import org.apache.spark.rdd.RDD




object ContentBased {

  val sparkContext: SparkContext = Infrastructure.sparkContext


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

    val userWeights = usersRatings
      .map(v => (v._1, removeZeroLines(v._2, itemMatrixBreeze )))
      .map(v => (v._1, v._2._1 \ v._2._2))


    val testRatings = sparkContext.textFile("ml-100k/u1.test")
      .map(_.split("\t") match {
        case Array(user, item, rate, timestamp) => Rating(user.toInt, item.toInt, rate.toDouble)
      }).cache()

    // remove rating from dataset
    val usersProducts = testRatings.map {
      case Rating(user, product, rate) => (user, product)
    }

    // for each user predict an then join
//    usersProducts.first()._2.dot(toBreeze(itemMatrix)(0))

   // val item = getRow(itemMatrix, 1)




    val temp = usersRatings.first()
    val tempUser: Int = 770
    val tempMatrix = temp._2

    val weight: DenseMatrix[Double] = userWeights.filter(v => v._1==tempUser).map(v=> v._2).first()
    println(weight.toArray.deep.mkString(","))

    val row: DenseMatrix[Double] = getRow(itemMatrix,270)

//    val x: DenseMatrix[Double] = row.t * tempMatrix


  }
  def removeZeroLines(userMatrix: DenseMatrix[Double], itemMatrix:DenseMatrix[Double]): (DenseMatrix[Double], DenseMatrix[Double]) = {
    val localItemMatrix = itemMatrix.copy
    val localUserMatrix = userMatrix.copy
    userMatrix.foreachKey { v =>
      if (userMatrix(v._1,v._2) == 0) {
        localUserMatrix.delete(v._1, Axis._0)
        localItemMatrix.delete(v._1, Axis._0)
      }
    }
    (localUserMatrix, localItemMatrix)
  }


  def getRow(matrix: Matrix, row: Int): DenseMatrix[Double] = {
    val numberOfColumns = matrix.numCols
    val array = new Array[Double](numberOfColumns)
    for (i <- 0 until matrix.numCols){
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
