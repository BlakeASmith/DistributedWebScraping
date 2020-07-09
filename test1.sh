(python routing.py)& routing=$!
(go run ./src/*.go)&> test1_out.log& server=$!
sleep 1 # give some time for server to start

java -jar ./DistributedWebScraper/build/DistributedWebScraper-1.0-SNAPSHOT.jar > /dev/null& client1=$!
java -jar ./DistributedWebScraper/build/DistributedWebScraper-1.0-SNAPSHOT.jar > /dev/null& client2=$!

trap "kill -9 $client1 $client2 $server $routing" SIGINT # make sure child proccesses killed on CTRL+c
wait $client1 $client2 # keep active until clients finish

