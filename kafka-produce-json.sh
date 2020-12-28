#!/bin/bash

KAFKA_BIN_PATH="./kafka_2.12-2.4.1/bin"
PRODUCER="$KAFKA_BIN_PATH/kafka-console-producer.sh"
BROKER_LIST="localhost:9092"
TOPIC="flink-topic-in"

cmd() {
  local timestamp="$(date +"%s")"
  local data="$(jo timestamp="${timestamp}" deviceId="$((RANDOM % 3 + 1))" temperature="$((RANDOM % 40)).$((RANDOM % 10))" humidity="$((RANDOM % 100)).$((RANDOM % 10))")"
  echo ${timestamp} '-' $data
  echo $data | $PRODUCER --broker-list $BROKER_LIST --topic $TOPIC 1>/dev/null
}

while :
do
  cmd
  sleep 1
done
