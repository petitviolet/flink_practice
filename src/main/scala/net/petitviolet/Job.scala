package net.petitviolet

import java.util.Properties

import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer

/**
 * {{{
 *   sbt clean assembly
 * }}}
 * will generate target/scala-2.11/Flink\ Project-assembly-0.1-SNAPSHOT.jar
 */
object Job {
  def main(args: Array[String]): Unit = {
    // set up the execution environment
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val properties = new Properties()
    properties.setProperty("bootstrap.servers", "localhost:32768")
    properties.setProperty("group.id", "test")

    val kafkaStream: DataStream[String] = env.addSource(new FlinkKafkaConsumer[String]("flink-topic", new SimpleStringSchema(), properties))

    val counts = kafkaStream.flatMap { _.split(' ') }
      .map { str => (str, 1) }
      .keyBy { _._1 }
      .sum(1)
    counts.print()
    // execute program
    env.execute("Flink Scala API Skeleton")
  }
}
