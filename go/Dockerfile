FROM golang:1.13
# using go.mod
RUN unset GOROOT 

ENV GO111MODULE=on 

# COPY ./install.sh .
# RUN ./install.sh .

RUN mkdir webscraper 
WORKDIR webscraper

COPY src/go.mod .
COPY src/go.sum .
RUN go mod download

COPY . .

RUN export 
RUN (cd src; go build -o $GOPATH/bin/webscraper)

ENTRYPOINT  ["./entrypoint.sh"]
