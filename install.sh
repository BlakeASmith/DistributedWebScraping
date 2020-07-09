#!/bin/sh
export GO111MODULE=on  # Enable module mode
go get -v google.golang.org/grpc
go get -v google.golang.org/genproto/googleapis/rpc/status
go get -v github.com/golang/protobuf/protoc-gen-go
go get -v github.com/golang/protobuf/protoc-gen-go-grpc
go get -v google.golang.org/grpc
go get -v golang.org/x/sys/unix
go get -v golang.org/x/text/secure/bidirule
go get -v golang.org/x/text/unicode/bidi
go get -v golang.org/x/text/unicode/norm
go get -v github.com/golang/protobuf/proto

# for jquery like html parsing
go get github.com/PuerkitoBio/goquery
