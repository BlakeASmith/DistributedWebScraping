#(python routing.py)& routing=$!
go run ./src/*.go& server=$!
sleep 2 # give some time for server to start

java -jar ./DistributedWebScraper/build/DistributedWebScraper-1.0-SNAPSHOT.jar > /dev/null& client1=$!
java -jar ./DistributedWebScraper/build/DistributedWebScraper-1.0-SNAPSHOT.jar > /dev/null& 
java -jar ./DistributedWebScraper/build/DistributedWebScraper-1.0-SNAPSHOT.jar > /dev/null& 

trap "kill -9 $client1 $client2 $server $routing" SIGINT # make sure child proccesses killed on CTRL+c
wait $client1 # keep active until clients finish

pkill -9 $server
pkill -9 $routing

