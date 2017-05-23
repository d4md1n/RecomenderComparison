
import java.util

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
    //  itemMatrix.toRowMatrix().rows.foreach(x => println(x))


    val ratings = sparkContext.textFile("ml-100k/u1.base")
      .map(_.split("\t") match {
        case Array(user, item, rate, timestamp) => Rating(user.toInt, item.toInt, rate.toDouble)
    }).cache()

    val userItemRating = ratings.groupBy(r => r.user)
      .map(v => (v._1, generateUserMatrix(v._2)))


    val x = toBreeze(userItemRating.first()._2) \ toBreeze(itemMatrix)
    println(x.data.deep.mkString("\n"))

    //val test = userItemRating.first()._2.multiply(inverseItemMatrix)

//      .map(v => (v._1, itemMatrix
//        .multiply(v._2))
//      )

//    println(test.numCols)
//    println(test.numRows)


//
//
//    userItemRating.foreach(v => {
//      println(v._1)
//      println(v._2.numCols())
//      println(v._2.numRows())
//    })


//    itemMatrix.toBlockMatrix().multiply
//
//    itemMatrix.entries.foreach(x => println(x))
//    println(itemMatrix.numCols())
//    println(itemMatrix.numRows())






  }

  def generateUserMatrix(userRatings: Iterable[Rating]): Matrix = {

    val numberOfItems = Infrastructure.items.count().toInt
    val array = new Array[Double](numberOfItems)
    util.Arrays.fill(array, 0)
    userRatings.foreach(r => array(r.product - 1) = 1)
//    println(array.deep.mkString("\n"))
    Matrices.dense(numberOfItems ,1, array)
  }


  //  private def getMetricsForDataset(trainingSet: String, testingSet: String) = {
  //    ("trainingSet", "testingSet", "MSE", "RMSE", "MAE", "executionTime")
  //
  //  }


  private def toBreeze(matrix: Matrix)= {
    if (!matrix.isTransposed) {
      new BDM(matrix.numRows, matrix.numCols, matrix.toArray)
    } else {
      val breezeMatrix = new BDM(matrix.numRows, matrix.numCols, matrix.toArray)
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
