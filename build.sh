#!/bin/bash

# this script will perform a build for each component and put the resulting JAR files under the
# build direcectry

./link.sh # link shared files

#perform builds
(cd plugins/PluginProvider; ./gradlew sourcesJar) 
(cd DistributedWebScraper; ./gradlew clientlib:sourcesJar ; ./gradlew sourcesJar) 
mkdir -p build

# copy jar files
cp plugins/PluginProvider/build/PluginProvider*.jar build/pluginprovider.jar
cp DistributedWebScraper/build/DistributedWebScraper*.jar build/client.jar
cp DistributedWebScraper/database/build/database*.jar build/database.jar
cp DistributedWebScraper/clientlib/build/clientlib*.jar build/clientlib.jar
