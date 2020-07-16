#!/bin/sh
# A pre-commit hook for git

# symlink proto files into each project
(cd ./DistributedWebScraper/src/main/proto; ln -f ../../../../proto/* .)
(cd ./DistributedWebScraper/database/src/main/proto; ln -f ../../../../../proto/* .)
(cd ./DistributedWebScraping-Android/app/src/main/proto/; ln -f ../../../../../proto/* .)

# symlink shared files into each project, have to cd into the appropriate directory for symlinks to work
(cd ./DistributedWebScraper/src/main/kotlin ; ln -f ../../../../shared/kt/* .)
(cd ./DistributedWebScraping-Android/app/src/main/java/com/example/distributedwebscraping/; ln -f ../../../../../../../../shared/kt/* .)


