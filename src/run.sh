#!/bin/sh

protoc \
  --proto_path=../proto \
  --go_out=proto \
  --go-grpc_out=proto \
  $(find ../proto -iname "*.proto")

go build proto/*.go
go build main.go server.go scraping.go
go run main.go server.go scraping.go
