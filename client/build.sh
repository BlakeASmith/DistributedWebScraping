#!/bin/sh

(cd ../kafka && ./gradlew jar)
(cd ../clientlib && ./gradlew jar)
./gradlew jar
sudo docker build -t blakeasmith/kafka_webscraper_client .
