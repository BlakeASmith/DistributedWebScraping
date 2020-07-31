# Distributed Web Scraping

A distributed web scraping framework which allows for multiple tasks to be 
run in parallel over an automatically scalable cluster of nodes, including android devices.

![archetecture diagram text](docs/kafka_archetecture.pdf "Archetecture Diagram")

The project is made up of serveral components:

1. A kafka cluster
2. A
1. A producer which crawls 
3. A (Cassandra) database service (Kotlin program & Cassandra database)
4. A routing service for discovery of ip addresses (Python & Flask)

