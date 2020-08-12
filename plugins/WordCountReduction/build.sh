#!/bin/sh
./gradlew jar
sudo docker build -t blakeasmith/webscraper_reducer_wordcount .
