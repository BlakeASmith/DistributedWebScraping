#!/bin/sh

./gradlew jar
sudo docker build -t blakeasmith/webscraper_proxy_client .

[ $1 == "push" ] && sudo docker push blakeasmith/webscraper_proxy_client 
