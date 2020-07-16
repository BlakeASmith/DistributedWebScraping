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
	proto.UnimplementedDatabaseServer

	IPAddress Address
	currentLeaderAddress Address
	RoutingServiceAddress string
	peers []Server

	JobChannel chan proto.Job

}

func (server *Server) RequestJob(context context.Context, request *proto.JobRequest) (*proto.Job, error) {
	job := <-server.JobChannel
	fmt.Println("Job Assigned", job)
	return &job, nil
}

func (server *Server) CompleteJob(context context.Context, job *proto.JobResult) (*proto.JobCompletion, error) {
	fmt.Println("Job Completed")
	fmt.Println(len(job.Results))
	return &proto.JobCompletion{}, nil
}


func (server *Server) Store(context context.Context, json *proto.JsonObjects) (*proto.StorageConfirmation, error) {
	fmt.Println("stored results")
	return &proto.StorageConfirmation{Success:true}, nil
}

// send IP addresses to the routing service so that new mobile clients
// can join the cluster
func (server *Server) sendUpdateToRoutingService() {
	log.Println("Sending address to routing service")

	response, err := http.Get(fmt.Sprintf(
		"%s/update/%s/%d",
		server.RoutingServiceAddress,
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
		//proto.RegisterDatabaseServer(gserver, server)
		fmt.Println("Server live at ", server.IPAddress)
		gserver.Serve(lis)
	} else {
		log.Fatal(err)
	}

}

