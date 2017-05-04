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
    val users = sc.textFile("ml-100k/u.user").map(u => u.trim.split("\\|")).cache()
    val genres = sc.textFile("ml-100k/u.genre").map(u => u.trim.split("\\|")).cache()
    val items = sc.textFile("ml-100k/u.item").map(u => u.trim.replace("||", "|").split("\\|")).cache()
    val occupations = sc.textFile("ml-100k/u.occupation").cache()
    val rattings = sc.textFile("ml-100k/u.data").map(u => u.trim.split(" ")).cache()

    val femaleUsers = users.filter(u => u(2) == "F").count()

    println("----------------------------")
    println(users.count())
    println(genres.count())
    println(items.count())
    println(occupations.count())
    println(rattings.count())
    println("----------------------------")
    println(femaleUsers)
    println(items.first()(3))
    println("----------------------------")

  }
}