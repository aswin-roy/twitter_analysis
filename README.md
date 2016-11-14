# twitter_text_analysis

A spark-streaming job to analyse Twitter feed using any filter.

Edit com.zerokool.twitter_analysis.constants.Constants class to add your twitter credentials and google maps key.

- To build :

sbt assembly

- To run:

spark-submit --class com.zerokool.twitter_analysis.appn.Application --master "master-url" path/to/target/scala-2.10/twitter_test-assembly-1.0.jar