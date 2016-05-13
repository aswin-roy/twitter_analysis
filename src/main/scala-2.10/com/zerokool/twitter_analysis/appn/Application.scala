package com.zerokool.twitter_analysis.appn

import com.zerokool.twitter_analysis.constants.Constants
import com.zerokool.twitter_analysis.core.{GeoUtils, TextUtils}
import org.apache.spark.mllib.classification.NaiveBayes
import org.apache.spark.mllib.feature.{HashingTF, IDF}
import org.apache.spark.mllib.linalg.Vector
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
    val conf = new SparkConf().setAppName("twitterIsTa")
    val sc = new SparkContext(conf)

    // Twitter Authentication credentials
    System.setProperty("twitter4j.oauth.consumerKey", Constants.CONSUMER_KEY)
    System.setProperty("twitter4j.oauth.consumerSecret", Constants.CONSUMER_SECRET)
    System.setProperty("twitter4j.oauth.accessToken", Constants.ACCESS_TOKEN)
    System.setProperty("twitter4j.oauth.accessTokenSecret", Constants.ACCESS_TOKEN_SECRET)

    val ssc = new StreamingContext(sc, Seconds(5))
    val twitterStream = TwitterUtils.createStream(ssc, None)

    val statuses = twitterStream.filter(status => status.getLang.equals("en")) //filter only the english tweets
      .map(status => {
      val textUtils = new TextUtils
      val geoUtils = new GeoUtils
      geoUtils.enrichLocationDetails(status.getUser.getLocation) //enriches geo location data
      textUtils.findSentiment(status.getText)   //finds sentiment of the tweet
      textUtils.removeStopWords(status.getText) //removes stop-words and displays filtered words
      status.getText
    })

    statuses.foreachRDD(
      rdd => {
        if(!rdd.isEmpty()){

          //training set
          val raw = sc.parallelize(Seq("I want to buy a tv,1","I want to buy a refrigerator,1",
            "sun rises in the east?,0", "What a day,0")) //training set should be ideally larger

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

          val predictions = test.map(p => (model.predict(p.features), p.label))

          println("prediction : ")
          predictions.collect().foreach( d => {
            println(d._1 + " : " + d._2)
          })

          predictions.zip(rdd).foreach(
            results => {
              if(results._1._1 == 1) {
                println("Tweet with buying intent : " + results._2)
              }
            }
          )

          //similarly recommendation/suggestion intent can also be detected using a binary classifier,
          // using the same steps. I am not implementing it to avoid repetition of same logic.

          val accuracy = 1.0 * predictions.filter(x => x._1 == x._2).count() / training.count()
          println("accuracy of predictions : "+accuracy)
          //to improve accuracy, the training set has to be larger

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
