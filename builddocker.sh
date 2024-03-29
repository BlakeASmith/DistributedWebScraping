#!/bin/sh

(cd client; ./build.sh;)
(cd provider; ./build.sh;)
(cd go; ./build.sh;)
(cd ClientProxyServer && ./build.sh && cd ProxyClient && ./build.sh)
(cd plugins/WordcountReducer && ./build.sh)

[ $1 == "push" ] && sudo docker push blakeasmith/kafka_webscraper_client \
		&& sudo docker push blakeasmith/kafka_webscraper_provider \
		&& sudo docker push blakeasmith/kafka_webscraper_producer \
		&& sudo docker push blakeasmith/webscraper_proxy_server \
		&& sudo docker push blakeasmith/webscraper_proxy_client \
		&& sudo docker push blakeasmith/webscraper_reducer_wordcount 
