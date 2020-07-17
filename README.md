# Distributed Web Scraping

Distributed web scraping project for csc 462.

## Install

Clone the repo

```bash
git clone https://github.com/BlakeASmith/DistributedWebScraping.git
sh link.sh # create symlinks for shared code
cp link.sh .git/hooks/pre-commit # do linking on pre-commit
sh install.sh # install dependencies
```

You may need to add the following to your .bashrc or .zshrc

```bash
export GOROOT=/usr/local/go
export GOPATH=$HOME/go
export GOBIN=$GOPATH/bin
```

For compiling the proto files, make sure to manually install the [protoc compiler](https://grpc.io/docs/protoc-installation/)

## Routing

Since the ip address of the RAFT leader will be changing, a routing service which is available
at a static endpoint will be needed to direct any added nodes to the correct server.

**routing.py** is a really simple flask app for this

## Execution

Run the routing.py file

```bash
python routing.py
```

Then run the server to start crawling

```bash
cd src
sh ./run.sh
```

To start some clients, compile the jar file for the scraper and then run the .jar file.

```bash
java -jar ./DistributedWebScraper/build/DistributedWebScraper-1.0-SNAPSHOT.jar > /dev/null&
```

The clients will then start sending `RequestJob` RPCs to the server to and execute its function.
