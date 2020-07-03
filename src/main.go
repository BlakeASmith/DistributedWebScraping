package main

import (
	"fmt"
	"net"
	"log"
	"google.golang.org/grpc"
	"./proto"
	"context"
)

type Server struct {
}

func (server *Server) RequestJob(context context.Context, request *proto.JobRequest) (*proto.Job, error) {
	fmt.Println("Job Completed")
	return nil, nil
}

func (server *Server) CompleteJob(context context.Context, job *proto.Job) (*proto.JobCompletion, error) {
	fmt.Println("Job Completed")
	return nil, nil
}


func (server *Server) start(address string) {
	if lis, err := net.Listen("tcp", address); err == nil {
		gserver := grpc.NewServer()
		proto.RegisterMasterServer(gserver, server)
		fmt.Println("Server live at ", address)
		gserver.Serve(lis)
	} else {
		log.Fatal(err)
	}
}


func main() {
	server := Server{}
	server.start("0.0.0.0:6969")
}
