
import org.apache.spark.mllib.linalg._
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.distributed.{CoordinateMatrix, MatrixEntry, RowMatrix}
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

    val itemMatrix = new CoordinateMatrix(itemsMatrixEntries).toRowMatrix()
    //  itemMatrix.toRowMatrix().rows.foreach(x => println(x))


    val ratings = sparkContext.textFile("ml-100k/u1.base").map(_.split("\t") match { case Array(user, item, rate, timestamp) =>
      Rating(user.toInt, item.toInt, rate.toDouble)
    }).cache()

    val userItemRating = ratings.groupBy(r => r.user)
      .map(v => (v._1, generateUserMatrix(v._2)))
      .map(v => (v._1, itemMatrix.multiply(v._2)))




//    itemMatrix.toBlockMatrix().multiply
//
//    itemMatrix.entries.foreach(x => println(x))
//    println(itemMatrix.numCols())
//    println(itemMatrix.numRows())






  }

  def generateUserMatrix(userRatings: Iterable[Rating]): Matrix = {

    val numberOfItems = (Infrastructure.items.count() - 1).toInt
    val temp = userRatings.map(r => (r.product, 0,r.rating)).toSeq
    SparseMatrix.fromCOO(numberOfItems, 0, temp)
  }


  //  private def getMetricsForDataset(trainingSet: String, testingSet: String) = {
  //    ("trainingSet", "testingSet", "MSE", "RMSE", "MAE", "executionTime")
  //
  //  }
  private def generateItemMatrixEntries = {
    Infrastructure.items.flatMap(a => Array(
    MatrixEntry(a(0).toLong, 0, a(4).toInt),
    MatrixEntry(a(0).toLong, 1, a(5).toInt),
    MatrixEntry(a(0).toLong, 2, a(6).toInt),
    MatrixEntry(a(0).toLong, 3, a(7).toInt),
    MatrixEntry(a(0).toLong, 4, a(8).toInt),
    MatrixEntry(a(0).toLong, 5, a(9).toInt),
    MatrixEntry(a(0).toLong, 6, a(10).toInt),
    MatrixEntry(a(0).toLong, 7, a(11).toInt),
    MatrixEntry(a(0).toLong, 8, a(12).toInt),
    MatrixEntry(a(0).toLong, 9, a(13).toInt),
    MatrixEntry(a(0).toLong, 10, a(14).toInt),
    MatrixEntry(a(0).toLong, 11, a(15).toInt),
    MatrixEntry(a(0).toLong, 12, a(16).toInt),
    MatrixEntry(a(0).toLong, 13, a(17).toInt),
    MatrixEntry(a(0).toLong, 14, a(18).toInt),
    MatrixEntry(a(0).toLong, 15, a(19).toInt),
    MatrixEntry(a(0).toLong, 16, a(20).toInt),
    MatrixEntry(a(0).toLong, 17, a(21).toInt),
    MatrixEntry(a(0).toLong, 18, a(22).toInt))
    )
  }
}
