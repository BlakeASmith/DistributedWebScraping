FROM golang:1.13

RUN unset GOROOT
WORKDIR /go/src/

COPY . .

RUN go build -o $GOPATH/bin/webscraper

EXPOSE 6969
ENTRYPOINT  ["webscraper"]
