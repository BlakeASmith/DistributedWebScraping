# Distributed Web Scraping

A distributed web scraping framework which allows for multiple tasks to be 
run in parallel over an automatically scalable cluster of nodes, including android devices.
In addition new functionality can be added at runtime via plugins (packaged in JAR files), allowing for 
many different workflows to be run over the cluster at the same time, and for new tasks to be performed 
on the cluster without going offline.

A diagram showing the basic architecture ![here](docs/kafka_archetecture.pdf "Architecture Diagram")

## Components

### Client Library

a library for defining plugins & services (how to scrape urls, and which urls to scrape)

	- create new plugins which define how to scrape data from the pages
	- define services to run over the cluster (start urls, illegal urls, etc)
	- collect results of a running service (via a Kafka topic)
	- see the ![README](clientlib/README.md "clientlib README")


### Crawling "Producer" Nodes

nodes which crawl pages and produce URLs to be scraped

- written in *Golang*
- receive service definitions at runtime via a Kafka topic 
- crawl the internet starting from the base domains specified for the service
- ensure **at most once** processing via a distributed hash table of discovered domains
	- separate hash table per service, shared between all nodes responsible for a given service
- produce jobs (groups of urls) to a kafka topic
- see the ![README](go/README.md "go README")

### Scraping "Client" Nodes

Client "scraper" nodes which take the URLs provided by the producer nodes and run scraping tasks over them

- written in Kotlin, desktop & *Android* clients available
- dynamically load plugins (which define scraping tasks) 
- produce JSON data to a different kafka topic (with the same name as the service) for each service
	- this is how the clients will receive the results
- see the ![README](client/README.md "client README")


### Logger (for Testing)

The ![logger](ResultLogger "logger README") reads from the output topics for all running services and logs the output for each to it's own file

### Provider

The ![provider](provider "provider README")/uploader service

- Reads plugin JARs from a directory and sends them to Kafka 
- Reads service definitions from a services.json file and sends them to Kafka

All components are containerized (via Docker) which will enable easy scaling. Test cases are run using **docker-compose**.

## Building the Docker Images 

The pre-built images are available on ![docker hub](https://hub.docker.com/u/blakeasmith "dockerhub"), but here is how you can build them locally:

To build all of the images at once simply run

```bash
./builddocker.sh
```

Each project has it's own *build.sh* script for building and tagging the images.
Note that all of the *Kotlin* projects are build to a jar file (by gradle) on the
host machine and then copied into the image. Otherwise all of the dependencies would have to 
be downloaded on each build of the container. The builds are ran using a gradle wrapper, so 
the correct version of gradle should be installed automatically.

## Running Test Cases

Test cases are ran via **docker-compose** and located under [testscripts/docker](testscripts/docker "tests").

Assuming **docker** and **docker-compose** are installed and running on your machine, you can run examples like this;

```bash
cd testscripts/docker/usedvic_wordcount
sudo docker-compose up
```

to scale the number of clients & kafka nodes you can use `--scale`

```bash
# run 3 clients and 3 kafka nodes
sudo docker-compose up --scale client=3 kafka=3
```


