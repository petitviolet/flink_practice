## Getting Started

Initiate a project using giter8 template [tillrohrmann/flink-project.g8](https://github.com/tillrohrmann/flink-project.g8).

```console
$ sbt new tillrohrmann/flink-project.g8`h
```

Launch a Kafka cluster using docker, and docker-compose.  
See [wurstmeister/kafka-docker][https://github.com/wurstmeister/kafka-docker] for further information.

```console
$ docker-compose up -d # launch Kafka
$ docker ps # see the port of Kafka broker
$ # create a topic for this example
$ ./kafka_2.12-2.4.1/bin/kafka-topics.sh --zookeeper localhost:2181 --create --topic flink-topic --partitions 1 --replication-factor 1
```

Next, launch a Flink cluster using CLI.

```console
$ wget https://.../flink-1.11.2/flink-1.11.2-bin-scala_2.12.tgz # see https://flink.apache.org/downloads.html
$ tar -xzf flink-1.11.2-bin-scala_2.12.tgz
$ ./flink-1.11.2/bin/start-cluster.sh
```

Then, build a JAR and submit it to the local Flink.

```console
$ sbt clean assembly
$ ../flink-1.11.2/bin/flink run ./target/scala-2.12/flink_practice-assembly-0.1-SNAPSHOT.jar
```

Now ready to use the streaming processing application. Send messages to the Kafka to process the messages.

```console
$ ./kafka_2.12-2.4.1/bin/kafka-console-producer.sh --broker-list localhost:32768 --topic flink-topic
```

The logs are available at 'localhost:8081'.
