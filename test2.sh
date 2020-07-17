#!/bin/sh

go run ./src/*.go $1 & server=$!
java -jar ./build/database.jar $1 & db=$!
sleep 2 # give some time for server to start

java -jar ./build/client.jar > /dev/null& client1=$!

trap "kill -9 $client1 $client2 $server $db" SIGINT # make sure child proccesses killed on CTRL+c
wait $client1 # keep active until clients finish

pkill -9 $server
pkill -9 $db

