package net.petitviolet

import org.apache.flink.api.common.serialization.SerializationSchema
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode

import scala.util.{ Failure, Success, Try }

object SensorData {
  def deserialize(obj: ObjectNode): Option[SensorData] = {
    Try {
      val value = obj.get("value")
      println(s"value: ${value}")
      SensorData(
        value.get("deviceId").asLong(),
        value.get("temperature").asDouble(),
        value.get("humidity").asDouble(),
        Some(Option(value.get("timestamp")).get.asLong())
      )
    } match {
      case Success(x) => Some(x)
      case Failure(exception) => {
        println(s"failed to parse JSON. obj = $obj, exception = $exception")
        None
      }
    }
  }

}
case class SensorData(deviceId: Long, temperature: Double, humidity: Double, timestamp: Option[Long])


object SensorAggregatedResult {
  val serializationSchema: SerializationSchema[SensorAggregatedResult] = ???
}

case class SensorAggregatedResult(deviceId: Long, count: Int, temperatureList: Seq[Double], humidityList: Seq[Double]) {
  private val avgTemperature = temperatureList.sum / count
  private val avgHumidity = humidityList.sum / count
  private val map: Map[String, Any] = (
    ("deviceId", deviceId)
      :: ("count", count)
      :: ("temperatureList", temperatureList)
      :: ("avgTemperatures", avgTemperature)
      :: ("humidityList", humidityList)
      :: ("avgHumidity", avgHumidity)
      :: Nil
    ).toMap

  def serialize: String = {
    // serialization
    util.parsing.json.JSONObject(map).toString()
  }
}
