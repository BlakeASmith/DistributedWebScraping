#!/bin/sh

# generate proto files
protoc \
  --proto_path=../proto \
  --go_out=plugins=grpc:./proto \
  $(find ../proto -iname "*.proto")

# build gRPC files
go build proto/*.go

# build the execution components for the server
go build main.go server.go scraping.go

# execute the server and specify the ip address
go run main.go server.go scraping.go $1
