#!/bin/bash

KAFKA_BIN_PATH="./kafka_2.12-2.4.1/bin"
PRODUCER="$KAFKA_BIN_PATH/kafka-console-producer.sh"
BROKER_LIST="localhost:32770"
TOPIC="flink-topic"

cmd() {
  data=$(jo deviceId="$((RANDOM % 3 + 1))" temperature="$((RANDOM % 40)).$((RANDOM % 10))" humidity="$((RANDOM % 100)).$((RANDOM % 10))")
  echo $(date +"%s") '-' $data
  echo $data | $PRODUCER --broker-list $BROKER_LIST --topic $TOPIC 1>/dev/null
}

while :
do
  cmd
  sleep 0.5
done
