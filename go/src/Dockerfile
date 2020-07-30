FROM golang:1.13

RUN unset GOROOT

WORKDIR go/src

COPY . .

RUN export WEBSCRAPER_CONFIG="$HOME/go/src/config.json"

RUN go build -o $GOPATH/bin/webscraper

ENTRYPOINT  ["webscraper"]
