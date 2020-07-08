(python routing.py)& routing=$!
(go run ./src/*.go)& server=$!

java -jar ./DistributedWebScraper/build/DistributedWebScraper-1.0-SNAPSHOT.jar&
java -jar ./DistributedWebScraper/build/DistributedWebScraper-1.0-SNAPSHOT.jar&
java -jar ./DistributedWebScraper/build/DistributedWebScraper-1.0-SNAPSHOT.jar

pkill $pid
pkill $routing

