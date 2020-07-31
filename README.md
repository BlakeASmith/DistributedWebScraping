# Distributed Web Scraping

A distributed web scraping framework which allows for multiple tasks to be 
run in parallel over an automatically scalable cluster of nodes, including android devices.
In addition new functionality can be added at runtime via plugins (packaged in JAR files), allowing for 
many different workflows to be run over the cluster at the same time, and for new tasks to be performed 
on the cluster without going offline.

A diagram showing the basic architecture ![here](docs/kafka_archetecture.pdf "Architecture Diagram")

The project is made up of several components:

1. A client library for defining plugins & services (how to scrape urls, and which urls to scrape)
	- create new plugins which define how to scrape data from the pages
	- define services to run over the cluster (start urls, illegal urls, etc)
	- collect results of a running service (via a Kafka topic)
2. Producer nodes which crawl pages and produce URLs to be scraped
	- receive service definitions at runtime via a Kafka topic 
	- crawl the internet starting from the base domains specified for the service
	- ensure **at most once** processing via a distributed hash table of discovered domains
		- separate hash table per service, shared between all nodes responsible for a given service
	- produce jobs (groups of urls) to a kafka topic

3. Client "scraper" nodes which take the URLs provided by the producer nodes and run scraping tasks over them
   	- dynamically load plugins (which define scraping tasks) 
	- produce JSON data ti 
3. A (Cassandra) database service (Kotlin program & Cassandra database)
4. A routing service for discovery of ip addresses (Python & Flask)

