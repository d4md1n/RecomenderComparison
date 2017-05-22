import org.apache.spark.rdd.RDD

object Metrics {

  def getMSE (ratesAndPredictions: RDD[((Int, Int), (Double, Double))] ): Double =  {
    ratesAndPredictions.map { case ((user, product), (r1, r2)) =>
      val err = r1 - r2
      err * err
    }.mean()
  }

  def getMAE (ratesAndPredictions: RDD[((Int, Int), (Double, Double))] ): Double =  {
    ratesAndPredictions.map { case ((user, product), (r1, r2)) =>
      val err = r1 - r2
      Math.abs(err)
    }.mean()
  }
}
