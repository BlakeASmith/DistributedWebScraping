#!/bin/sh
# A pre-commit hook for git

# copy proto files into each project
cp ./proto/* ./DistributedWebScraper/src/main/proto
cp ./proto/* ./DistributedWebScraping-Android/app/src/main/proto

# copy shared configuration into each project
cp ./shared_config.json ./DistributedWebScraper/src/main/
cp ./shared_config.json ./DistributedWebScraping-Android/app/src/main/
