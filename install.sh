#!/bin/sh
export GO111MODULE=on  # Enable module mode
go get -v -u google.golang.org/grpc
go get -v -u google.golang.org/genproto/googleapis/rpc/status
go get -v -u github.com/golang/protobuf/protoc-gen-go
go get -v -u github.com/golang/protobuf/protoc-gen-go-grpc
go get -v -u google.golang.org/grpc
go get -v -u golang.org/x/sys/unix
go get -v -u golang.org/x/text/secure/bidirule
go get -v -u golang.org/x/text/unicode/bidi
go get -v -u golang.org/x/text/unicode/norm
go get -v -u github.com/golang/protobuf/proto

# for jquery like html parsing
go get -v -u github.com/PuerkitoBio/goquery
go get github.com/gocql/gocql
go get github.com/hailocab/gocassa
