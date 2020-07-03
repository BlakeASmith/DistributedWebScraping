package main

import (
	"fmt"
	"net"
	"log"
	"google.golang.org/grpc"
	"./proto"
	"context"
	"net/http"
	"io/ioutil"
)

type Address struct {
	IP string
	Port int
}


type Server struct {
	proto.UnimplementedMasterServer

	IPAddress Address
	currentLeaderAddress Address
	peers []Server

	RoutingServiceAddress Address
}

func (server *Server) RequestJob(context context.Context, request *proto.JobRequest) (*proto.Job, error) {
	fmt.Println("Job Requested")
	return nextJob(), nil
}

func (server *Server) CompleteJob(context context.Context, job *proto.JobResult) (*proto.JobCompletion, error) {
	fmt.Println("Job Completed")
	return &proto.JobCompletion{}, nil
}

func (server *Server) mustEmbedUnimplementedMasterServer() {

}

// send IP addresses to the routing service so that new mobile clients
// can join the cluster
func (server *Server) sendUpdateToRoutingService() {
	log.Println("Sending address to routing service")

	response, err := http.Get(fmt.Sprintf(
		"http://%s:%d/update/%s/%d",
		server.RoutingServiceAddress.IP,
		server.RoutingServiceAddress.Port,
		server.IPAddress.IP,
		server.IPAddress.Port,
	))

	if err != nil {
		panic(err)
	}

	defer response.Body.Close()

	body, err := ioutil.ReadAll(response.Body)
	if err != nil {
		panic(err)
	}


	log.Println(string(body))
}

func (server *Server) start() {
	if lis, err := net.Listen("tcp", fmt.Sprintf("%s:%d", server.IPAddress.IP, server.IPAddress.Port)); err == nil {
		gserver := grpc.NewServer()
		proto.RegisterMasterServer(gserver, server)
		fmt.Println("Server live at ", server.IPAddress)
		gserver.Serve(lis)
	} else {
		log.Fatal(err)
	}

}

