package com.twitter.ista.appn

import com.twitter.ista.core.{GeoUtils, TextUtils}
import org.apache.spark.ml.feature.StopWordsRemover
import org.apache.spark.mllib.classification.NaiveBayes
import org.apache.spark.mllib.feature.{IDF, HashingTF}
import org.apache.spark.mllib.linalg.{Vectors, Vector}
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

/**
  * Created by aswin on 6/5/16.
  */
object Application {

  def main(args: Array[String]) {
    println("starting...")
    val conf = new SparkConf().setAppName("twitterIsTa")
    val sc = new SparkContext(conf)

    // Twitter Authentication credentials
    System.setProperty("twitter4j.oauth.consumerKey", "gZR0Ftw9CMxkUhgDoKPAjWL8O")
    System.setProperty("twitter4j.oauth.consumerSecret","ZQqNFA255X6dGzEZvddMkcA6FBmGpfJtTR9M4PHFjVdGJZpjYC")
    System.setProperty("twitter4j.oauth.accessToken", "227630383-7w70plyjTE1AVLs9KaOcMeAN3dfKxlAscbVVP7mJ")
    System.setProperty("twitter4j.oauth.accessTokenSecret", "wIhVDs2d0V64eklzI7O2gwief12kxuC910ssIeEP5PMZD")
    println("wait...")

    val ssc = new StreamingContext(sc, Seconds(5))
    val twitterStream = TwitterUtils.createStream(ssc, None)

    val statuses = twitterStream.filter(status => status.getLang.equals("en")) //filter only the english tweets
            .map(status => (status.getText, status.getUser.getLocation))
            .map(sentence => {
              val textUtils = new TextUtils
//              val geoUtils = new GeoUtils
//              geoUtils.enrichLocationDetails(sentence._2) //enriches geo location data
//              textUtils.getSentiment(sentence._1)
              textUtils.removeStopWords(sentence._1)
            })

    statuses.print()

    statuses.foreachRDD(
      rdd => {
        if(!rdd.isEmpty()){

          val raw = sc.parallelize(Seq("I want to buy a tv,1","I want to buy a refrigerator,1",
            "I am going home,0", "What a day,0"))

          val labels = raw.map(string => {
            val parts = string.split(",")
            parts(1)
          })

          val trainingFeatures = raw.map(string => {
            val parts = string.split(",")
            parts(0).split(" ").toSeq
          })

          val htfTraining = new HashingTF()
          val tfTraining: RDD[Vector] = htfTraining.transform(trainingFeatures)
          tfTraining.cache()
          val idfTraining = new IDF().fit(tfTraining)
          val tfIdfTraining: RDD[Vector] = idfTraining.transform(tfTraining)

          val training = labels.zip(tfIdfTraining).map(zip => {
            new LabeledPoint(zip._1.toDouble, zip._2)
          })

          val testFeatures = rdd.map(string => {
            string.split(" ").toSeq
          })

          val htfTest = new HashingTF()
          val tfTest: RDD[Vector] = htfTest.transform(testFeatures)
          tfTest.cache()
          val idfTest = new IDF().fit(tfTest)
          val tfIdfTest: RDD[Vector] = idfTest.transform(tfTest)

          val test = tfIdfTest.map(vector => {
            new LabeledPoint(0, vector)
          })

          val model = NaiveBayes.train(training, lambda = 1.0, modelType = "multinomial")

          val p = test.map(p => (model.predict(p.features), p.label))

          println("prediction : ")
          p.collect().foreach( d => {
            println(d._1 + " : " + d._2)
          })

          val accuracy = 1.0 * p.filter(x => x._1 == x._2).count() / training.count()

          println("accuracy : "+accuracy)

        }

      }
    )

    sys.addShutdownHook{
      ssc.stop(stopSparkContext = true, stopGracefully = true)
    }

    ssc.start()
    ssc.awaitTermination()

  }
}
