#!/bin/sh

(cd client; ./build.sh; cd ..)
(cd provider; ./build.sh; cd ..)
(cd ResultLogger; ./build.sh; cd ..)
(cd go; ./build.sh; cd ..)

[ $1 == "push" ] && sudo docker push blakeasmith/kafka_webscraper_client \
		&& sudo docker push blakeasmith/kafka_webscraper_provider \
		&& sudo docker push blakeasmith/kafka_webscraper_producer \
		&& sudo docker push blakeasmith/kafka_webscraper_logger
