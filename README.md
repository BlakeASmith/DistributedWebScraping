# Distributed Web Scraping

Distributed web scraping project for csc 462.

## Install

Clone the repo using the `--recurse-submodules` tag

```bash
git clone --recurse-submodules https://github.com/BlakeASmith/DistributedWebScraping.git
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



## Routing

Since the ip address of the RAFT leader will be changing, a routing service which is available 
at a static endpoint will be needed to direct any added nodes to the correct server. 

**routing.py** is a really simple flask app for this
