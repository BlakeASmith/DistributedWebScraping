#!/bin/sh

cd .. 
if [ "$1" == "build" ]
then 
	./build.sh 
fi
cd build

# run spotify/kafka (which starts kafka and zookeeper) on localhost
name=$(sudo docker run -d --network host spotify/kafka)
trap "sudo docker kill $name" SIGINT

sudo docker ps
java -jar pluginprovider.jar --jars ../plugins/jars 
java -jar client.jar --loadonly

sudo docker kill $name
