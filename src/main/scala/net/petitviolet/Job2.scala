package net.petitviolet

import java.time.Duration
import java.util.Properties

import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.WindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.flink.streaming.util.serialization.JSONKeyValueDeserializationSchema
import org.apache.flink.util.Collector

import scala.util.{ Failure, Success, Try }

/**
 * {{{
 *   sbt clean assembly
 * }}}
 * will generate target/scala-2.11/Flink\ Project-assembly-0.1-SNAPSHOT.jar
 */
object Job2 {
  def main(args: Array[String]): Unit = {
    // set up the execution environment
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.getConfig.setAutoWatermarkInterval(1000L)

    val properties = new Properties()
    properties.setProperty("bootstrap.servers", "localhost:32770")
    properties.setProperty("group.id", "test")

    val kafkaStream: DataStream[ObjectNode] = env.addSource(
      new FlinkKafkaConsumer("flink-topic",
        new JSONKeyValueDeserializationSchema(false),
        properties,
      )
    ).assignTimestampsAndWatermarks(
      WatermarkStrategy.forBoundedOutOfOrderness[ObjectNode](Duration.ofSeconds(5))
    )

    val deserializedStream = kafkaStream.flatMap { obj: ObjectNode =>
      Try {
        val value = obj.get("value")
        SensorData(
          value.get("deviceId").asLong(),
          value.get("temperature").asDouble(),
          value.get("humidity").asDouble(),
        )
      } match {
        case Success(x) => Some(x)
        case Failure(exception) => {
          println(s"failed to parse JSON. obj = $obj, exception = $exception")
          None
        }
      }
    }

    val processed: DataStream[SensorAggregatedResult] = deserializedStream.keyBy { _.deviceId }
      .timeWindow(Time.seconds(20))
      .apply {
        new WindowFunction[SensorData, SensorAggregatedResult, Long, TimeWindow] {
          override def apply(
            key: Long,
            window: TimeWindow,
            input: Iterable[SensorData],
            out: Collector[SensorAggregatedResult]): Unit = {
            val (count, temps, hums) = input.foldLeft((0, Seq.empty[Double], Seq.empty[Double])) {
              case ((count, acc_tmp, acc_hum), data) =>
                (count + 1, data.temperature +: acc_tmp, data.humidity +: acc_hum)
            }
            out.collect(SensorAggregatedResult(key, count, temps, hums))
          }
        }
      }

    processed.print()
    // execute program

    env.execute("Flink Kafka JSON to average with timewindow")
  }
}

case class SensorData(deviceId: Long, temperature: Double, humidity: Double)

case class SensorAggregatedResult(deviceId: Long, count: Int, temperatures: Seq[Double], humidities: Seq[Double]) {
  private val avgTemperature = temperatures.sum / count
  private val avgHumidity = humidities.sum / count
  private val map: Map[String, Any] = (
    ("deviceId", deviceId)
      :: ("count", count)
      :: ("temperatures", temperatures)
      :: ("avgTemperatures", avgTemperature)
      :: ("humidities", humidities)
      :: ("avgHumidity", avgHumidity)
      :: Nil
    ).toMap

  override def toString: String = util.parsing.json.JSONObject(map).toString()
}