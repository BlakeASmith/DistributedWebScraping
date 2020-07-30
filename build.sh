#!/bin/bash

# this script will perform a build for each component and put the resulting JAR files under the
# build direcectry

#perform builds
(cd plugins/PluginProvider; ./gradlew shadowJar) 
(cd clientlib; ./gradlew jar)
(cd client; ./gradlew jar) 
mkdir -p build

# copy jar files
cp plugins/PluginProvider/build/libs/PluginProvider*.jar build/pluginprovider.jar
cp client/build/libs/client*.jar build/client.jar
cp clientlib/build/libs/clientlib*.jar build/clientlib.jar
