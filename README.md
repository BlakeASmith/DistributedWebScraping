# Distributed Web Scraping

Distributed web scraping project for csc 462.

## Install

Clone the repo using the `--recurse-submodules` tag

```bash
git clone --recurse-submodules https://github.com/BlakeASmith/DistributedWebScraping.git
```

## Routing

Since the ip address of the RAFT leader will be changing, a routing service which is available 
at a static endpoint will be needed to direct any added nodes to the correct server. 

**routing.py** is a really simple flask app for this
