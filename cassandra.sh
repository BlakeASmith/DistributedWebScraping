#!/bin/bash
# use java version 8 
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk/jre/; shift
PATH=${JAVA_HOME}/bin:$PATH
sudo cassandra -R
