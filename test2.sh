#!/bin/sh

go run ./src/*.go $1 & server=$!
java -jar ./DistributedWebScraper/database/build/database-1.0-SNAPSHOT.jar $1 & db=$!
sleep 2 # give some time for server to start

java -jar ./DistributedWebScraper/build/DistributedWebScraper-1.0-SNAPSHOT.jar > /dev/null& client1=$!
java -jar ./DistributedWebScraper/build/DistributedWebScraper-1.0-SNAPSHOT.jar > /dev/null& 

trap "kill -9 $client1 $client2 $server $db" SIGINT # make sure child proccesses killed on CTRL+c
wait $client1 # keep active until clients finish

pkill -9 $server
pkill -9 $db

