import org.apache.spark.{Partition, SparkContext, TaskContext}
import org.apache.spark.mllib.linalg.distributed.{BlockMatrix, CoordinateMatrix, MatrixEntry}
import org.apache.spark.rdd.RDD

import scala.collection.mutable.ListBuffer

object ContentBased {

  val sparkContext: SparkContext = Infrastructure.sparkContext

  def main(args: Array[String]) {
//    Infrastructure.dataSetList
//      .map(dataSet => getMetricsForDataset(dataSet._1, dataSet._2))
//      .foreach(metric => println(metric))
//    println("training set", "testing set", "MSE", "RMSE", "MAE", "Execution Time")

    val itemsMatrixEntries = Infrastructure.items.flatMap(a => Array(
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

    val itemMatrix = new CoordinateMatrix(itemsMatrixEntries)
  //  itemMatrix.toRowMatrix().rows.foreach(x => println(x))

    println(itemMatrix.numCols())
    println(itemMatrix.numRows())





  }

//  private def getMetricsForDataset(trainingSet: String, testingSet: String) = {
//    ("trainingSet", "testingSet", "MSE", "RMSE", "MAE", "executionTime")
//
//  }
}