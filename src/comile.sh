#!/bin/sh

protoc \
  --proto_path=../proto \
  --go_out=. \
  --go-grpc_out=. \
  $(find ../proto -iname "*.proto")

go build main.go
go build proto/app.pb.go
