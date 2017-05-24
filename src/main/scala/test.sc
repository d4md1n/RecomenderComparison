import breeze.linalg.{Axis, DenseMatrix}
val movies = Array(1,2,1,2,0,3,4,5,3,0,4,5) //movies4 / users3
//each movie has 2 attributes
val movie1 = Array(1,0)
val movie2 = Array(1,1)
val movie3 = Array(0,1)
val movie4 = Array(1,1)
val movie5 = Array(0,1)

DenseMatrix(movies).data.deep.mkString(",")
val moviesMatrix = new DenseMatrix(4,3,movies)

val moviesMatrix2 = moviesMatrix.copy

moviesMatrix.delete(2, Axis._0) // delete row

val moviesMatrix2Iterator = moviesMatrix2.activeIterator


moviesMatrix2.foreachKey { v =>
  println(v)
}