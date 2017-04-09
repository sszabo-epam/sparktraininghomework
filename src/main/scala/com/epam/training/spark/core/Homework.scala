package com.epam.training.spark.core

import com.epam.training.spark.core.domain.Climate
import org.apache.spark.rdd.RDD
import org.apache.spark.{Partition, SparkConf, SparkContext, TaskContext}
import org.codehaus.jackson.map.ext.JodaSerializers.DateTimeSerializer

object Homework {
  val DELIMITER = ";"
  val RAW_BUDAPEST_DATA = "data/budapest_daily_1901-2010.csv"
  val OUTPUT_DUR = "output"

  def main(args: Array[String]): Unit = {
    val sparkConf: SparkConf = new SparkConf()
      .setAppName("EPAM BigData training Spark Core homework")
      .setIfMissing("spark.master", "local[2]")
      .setIfMissing("spark.sql.shuffle.partitions", "10")
    val sc = new SparkContext(sparkConf)

    processData(sc)

    sc.stop()

  }

  def processData(sc: SparkContext): Unit = {

    /**
      * Task 1
      * Read raw data from provided file, remove header, split rows by delimiter
      */
    val rawData: RDD[List[String]] = getRawDataWithoutHeader(sc, Homework.RAW_BUDAPEST_DATA, Homework.DELIMITER)

    /**
      * Task 2
      * Find errors or missing values in the data
      */
    val errors: List[Int] = findErrors(rawData)
    println(errors)

    /**
      * Task 3
      * Map raw data to Climate type
      */
    val climateRdd: RDD[Climate] = mapToClimate(rawData)

    /**
      * Task 4
      * List average temperature for a given day in every year
      */
    val averageTemeperatureRdd: RDD[Double] = averageTemperature(climateRdd, 1, 2)

    /**
      * Task 5
      * Predict temperature based on mean temperature for every year including 1 day before and after
      * For the given month 1 and day 2 (2nd January) include days 1st January and 3rd January in the calculation
      */
    val predictedTemperature: Double = predictTemperature(climateRdd, 1, 2)
    println(s"Predicted temperature: $predictedTemperature")

  }

  def getRawDataWithoutHeader(sc: SparkContext, rawDataPath: String, delimeter: String): RDD[List[String]] = {
   val csv =sc.textFile(rawDataPath)
    csv
      .filter(!_.startsWith("#"))
      .map(_.split(";", 8).toList)
  }

  def findErrors(rawData: RDD[List[String]]): List[Int] = {
    rawData
      .map(_.map((value:String) => if (value == "") 1 else 0))
      .reduce((aggregate, item) => {
        aggregate
          .zip(item)
          .map(a => a._1 + a._2)
      })
  }

  def mapToClimate(rawData: RDD[List[String]]): RDD[Climate] = {
    rawData.map(item => Climate.apply(item(0), item(1), item(2), item(3), item(4), item(5), item(6)))
  }

  def averageTemperature(climateData: RDD[Climate], month: Int, dayOfMonth: Int): RDD[Double] = {
    climateData
      .filter(c => c.observationDate.getMonthValue == month && c.observationDate.getDayOfMonth == dayOfMonth)
      .map(c => c.meanTemperature.value)
  }

  def predictTemperature(climateData: RDD[Climate], month: Int, dayOfMonth: Int): Double = {
    var days = List((month,  dayOfMonth - 1), (month,  dayOfMonth), (month,  dayOfMonth + 1))
    var temperatures = days.flatMap(d => averageTemperature(climateData, d._1, d._2).toLocalIterator)
    temperatures.sum /  temperatures.length
  }


}


