#!/bin/sh
export GO111MODULE=on  # Enable module mode
go get -u -v google.golang.org/grpc
go get -u -v google.golang.org/genproto/googleapis/rpc/status
go get -u -v github.com/golang/protobuf/protoc-gen-go
go get -u -v github.com/golang/protobuf/protoc-gen-go-grpc
go get -u -v google.golang.org/grpc
go get -u -v golang.org/x/sys/unix
go get -u -v golang.org/x/text/secure/bidirule
go get -u -v  golang.org/x/text/unicode/bidi
go get -u -v golang.org/x/text/unicode/norm


