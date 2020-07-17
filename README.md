# Distributed Web Scraping

Distributed web scraping project which allows any computer with java(8), including Android
devices, to join the cluster on the fly. The project is made up of 4 major components.

1. A client program (Kotlin & Android)
2. A master service (Golang) which runs in a RAFT cluster
3. A (Cassandra) database service (Kotlin program & Cassandra database)
4. A routing service for discovery of ip addresses (Python & Flask)

## Installation

If you just want to run the project & are not contributing see the section on **Docker**

If contributing to any of the Kotlin services, you will need to install Intellij Idea.
This project is using intellij-idea-community-edition 2:2020.1.3-1.

If contributing to the Android application then install Android Studio, the current
version of the app was built using android-studio 4.0.0.16-1.

### Clone the repository and link shared files

```bash
git clone https://github.com/BlakeASmith/DistributedWebScraping.git
sh link.sh # create symlinks for shared code
cp link.sh .git/hooks/pre-commit # do linking on pre-commit
```

We are using synlinked files to share code between the Android app and Kotlin client
as it is much easier to deal with than doing it within the IDE. To add a shared file
between the projects;

```bash
touch shared/kt/<filename>.kt
./link.sh
```

The .proto files in the _proto_ directory are also linked into the intelij projects

### Installing go modules

You may need to add the following to your .bashrc or .zshrc

```bash
export GOROOT=/usr/local/go
export GOPATH=$HOME/go
export GOBIN=$GOPATH/bin
```

For compiling the proto files, make sure to manually install the [protoc compiler](https://grpc.io/docs/protoc-installation/)

Install the required go modules using `go build`

```bash
cd src && go install
```

If the service & message definitions in the _proto_ directory
are changed, you will need to run _src/run.sh_ which will build
the gRPC stubs before running the project.

you will need to install the **protoc** compiler prior to. see https://grpc.io/docs/protoc-installation/

### Cassandra

To run the project locally without Docker you will need to install Cassandra https://cassandra.apache.org/doc/latest/getting_started/installing.html.
Make sure you have java JDK 8 installed as well, cassandra will not work with a later version.

I'd recommend skipping this step and using the docker container (see the section on _Docker_)

### Building the project

To build the Kotlin client & Database service outside of the IDE (intellij);

```bash
sh build.sh
```

A _build_ directory will be produced containing _client.jar_ and _database.jar_

The jar files can then be executed using;

```bash
java -jar build/client.jar # for the client
# or java -jar build/database.jar <ip-address> for the database service
```

Cassandra will need to be running on port 9042 on localhost for the database service to work

## Running it all

To run the go master, database service, and client service all at once you can use
_test2.sh <ip-address>_

The routing service is deployed at http://blakesmith.pythonanywhere.com/ (which is currently hard-coded into each service)

### Docker

you can find pre-built docker images for each service at these repositories;

- go-master-service: https://hub.docker.com/repository/docker/blakeasmith/webscraper_go_master_service
- database service: https://hub.docker.com/repository/docker/blakeasmith/webscraper_database_service
- kotlin client: https://hub.docker.com/repository/docker/blakeasmith/webscraper_client

The database service image includes a cassandra installation and automatically runs it inside the container.

If on Ubuntu (specifically the EC2 ubuntu 18.04 AMI) you can install docker by running

```bash
sh -c "$(curl -s https://gist.githubusercontent.com/BlakeASmith/535086842ae134ead4c6aff5b97bea5e/raw/7d68bb28907fc312ef7671cc4252d89942a53041/install_docker.sh)"
```

This is useful for setting up cloud containers quickly via Amazons EC2 service. It will also set it up such that `sudo` is not
required before each docker command.

Then pull the images by running

```bash
docker pull blakeasmith/webscraper_client
docker pull blakeasmith/webscraper_database_service
docker pull blakeasmith/webscraper_go_master_service

```

To quickly run the whole shebang;

```bash
docker run -d --network host --name master blakeasmith/webscraper_go_master_service <ip-addr>
docker run -d --network host --name database blakeasmith/webscraper_database_service <ip-addr>
docker run -d --network host --name client blakeasmith/webscraper_client
```

for localhost, use <ip-addr> 127.0.0.1

If you instead use your public ip, then clients will automatically be able to connect via the routing service.

### Building the Docker images

To build the docker image for the client

```bash
./build.sh
cd DistributedWebScraping
docker build -t <tag name> -f clientDockerfile .
```

for the database service (& cassandra)

```bash
cd DistributedWebScraping
docker build -t <tag name> .
```

for the go master service

```bash
cd src
docker build -t <tag name> .
```
