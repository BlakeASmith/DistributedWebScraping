#!/bin/bash

./link.sh # link shared files
(cd DistributedWebScraper; ./gradlew sourcesJar) # perform gradle build
mkdir -p build
cp DistributedWebScraper/build/DistributedWebScraper*.jar build/client.jar
cp DistributedWebScraper/database/build/database*.jar build/database.jar
