import org.apache.spark.{SparkConf, SparkContext}
/**
  * Created by vasilis on 11/04/17.
  */
object MainObject {

  val conf = new SparkConf()
    .setMaster("local[4]")
    .setAppName("RecomenderComparison")
  val sc = new SparkContext(conf)

  def main(args: Array[String]) {
    val rdd = sc.textFile("steam-200k.csv").cache()
    val numberOfPurchases = rdd.filter(x =>  x.split(",")(3) == String.valueOf(1.00)).count()
    println("----------------------------")
    println(numberOfPurchases)
    println("----------------------------")

    val numberOfPlays:Double = rdd.filter(x =>  x.split(",")(3) != String.valueOf(1.00)).count()
    val sumOfPlays:Double = rdd.filter(x =>  x.split(",")(3) != String.valueOf(1.00))
      .map(y => y(3).toDouble).sum()
    println("----------------------------")
    println(sumOfPlays/numberOfPlays)
    println("----------------------------")
  }
}