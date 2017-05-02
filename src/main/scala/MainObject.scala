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

    val users = sc.textFile("ml-100k/u.user").cache()
    val genres = sc.textFile("ml-100k/u.genre").cache()
    val items = sc.textFile("ml-100k/u.item").cache()
    val occupations = sc.textFile("ml-100k/u.occupation").cache()
    val rattings = sc.textFile("ml-100k/u.data").cache()

    val femaleUsers = users.map(u=> u.split("|")).filter(u => u(2) == "F").count()

    println("----------------------------")
    println(users.count())
    println(genres.count())
    println(items.count())
    println(occupations.count())
    println(rattings.count())
    println("----------------------------")
    println(femaleUsers)
    println("----------------------------")

  }
}