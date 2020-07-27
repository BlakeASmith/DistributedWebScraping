#!/bin/sh

cd .. 
if [ "$1" == "build" ]
then 
	./build.sh 
fi
cd build
# run spotify/kafka (which starts kafka and zookeeper) on localhost
name=$(sudo docker run -d --network host spotify/kafka)

sudo docker ps

java -jar pluginprovider.jar ../plugins/jars 

sleep 10
java -jar client.jar --loadonly 

trap "sudo docker kill $name" SIGINT
sudo docker kill $name
