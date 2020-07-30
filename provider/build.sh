#!/bin/sh
./gradlew shadowJar
sudo docker build -t blakeasmith/kafka_webscraper_provider .
