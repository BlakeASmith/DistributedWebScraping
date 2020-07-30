#!/bin/sh


export WEBSCRAPER_CONFIG="../testscripts/config.json"

cd .. 
if [ "$1" == "build" ]
then 
	./build.sh 
fi
cd build


java -jar pluginprovider.jar --jars ../plugins/jars --services ../testscripts/services.json
#java -jar client.jar

sleep inf

