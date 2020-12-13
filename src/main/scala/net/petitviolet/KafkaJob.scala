package net.petitviolet

import java.time.Duration
import java.util.Properties

import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.function.WindowFunction
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.streaming.connectors.kafka.{ FlinkKafkaConsumer, FlinkKafkaProducer }
import org.apache.flink.streaming.util.serialization.JSONKeyValueDeserializationSchema
import org.apache.flink.util.Collector

/**
 * {{{
 *   sbt clean assembly
 * }}}
 * will generate target/scala-2.11/Flink\ Project-assembly-0.1-SNAPSHOT.jar
 */
object KafkaJob {
  def main(args: Array[String]): Unit = {
    // set up the execution environment
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.getConfig.setAutoWatermarkInterval(1000L)


    // $ ../flink-1.11.2/bin/flink run -c net.petitviolet.Job2 \
    //     ./target/scala-2.12/flink_practice-assembly-0.1-SNAPSHOT.jar \
    //     --bootstrap.servers localhost:32771 \
    //     --kafka-topic-in flink-topic-in \
    //     --kafka-topic-out flink-topic-out \
    //     --group.id test
    val parameterTool = ParameterTool.fromArgs(args)
    val properties = new Properties()
    properties.setProperty("bootstrap.servers", parameterTool.getRequired("bootstrap.servers"))
    properties.setProperty("group.id", parameterTool.getRequired("group.id"))

    val kafkaStream: DataStream[ObjectNode] = env.addSource(
      new FlinkKafkaConsumer(parameterTool.getRequired("kafka-topic-in"),
        new JSONKeyValueDeserializationSchema(false),
        properties,
      )
    ).assignTimestampsAndWatermarks(
      WatermarkStrategy.forBoundedOutOfOrderness[ObjectNode](Duration.ofSeconds(5))
    )

    val deserializedStream: DataStream[SensorData] = kafkaStream.flatMap { SensorData.deserialize(_) }

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

    val processedStrings: DataStream[String] = processed.map { result => result.serialize }

    val graph: DataStreamSink[String] = processedStrings.addSink(new FlinkKafkaProducer(
      parameterTool.getRequired("bootstrap.servers"),
      parameterTool.getRequired("kafka-topic-out"),
      new SimpleStringSchema()
    ))

    // execute program
    env.execute("sensor data calculation from kafka into kafka")
  }
}
