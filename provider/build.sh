#!/bin/sh
./gradlew jar
sudo docker build -t blakeasmith/kafka_webscraper_provider .
