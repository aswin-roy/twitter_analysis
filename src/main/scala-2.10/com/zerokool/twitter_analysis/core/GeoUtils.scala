package com.zerokool.twitter_analysis.core

import java.io.{BufferedReader, InputStreamReader}
import java.net.{HttpURLConnection, URL}

import com.zerokool.twitter_analysis.constants.Constants
import org.apache.commons.httpclient.util.URIUtil
import org.codehaus.jettison.json

/**
  * Created by aswin on 8/5/16.
  */
class GeoUtils {

  def enrichLocationDetails(location: String): Unit = {
    if(location != null){
      //the g-maps api has a limit of requests per day.
      val url = new URL("https://maps.googleapis.com/maps/api/geocode/json?address="
        + URIUtil.encodeQuery(location) + "&key="+ Constants.MAPS_KEY)
      val conn = url.openConnection().asInstanceOf[HttpURLConnection]

      conn.setRequestMethod("GET")
      conn.setRequestProperty("Accept", "application/json")

      if(conn.getResponseCode != 200) {
        println("Issue getting connection.")
      }
      val br = new BufferedReader(new InputStreamReader(conn.getInputStream))
      var fullResult = ""

      Iterator.continually(br.readLine()).takeWhile(_ ne null).foreach(s => {
        fullResult = fullResult.concat(s)
        println(s)
      })

      val jsonObj = new json.JSONObject(fullResult)
      val status = jsonObj.getString("status")

      //check if status is OK , else not OK
      if(status.equals("OK")) {
        println("Location details in the json : ")
        println(fullResult)
      }
      else println("Not a valid location name.")
    }

  }
}
