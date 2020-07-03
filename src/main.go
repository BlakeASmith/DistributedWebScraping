package main


func main() {
	RoutingServiceAddress :=  Address { IP: "0.0.0.0", Port: 5000 }
	server := Server{ IPAddress: Address{ IP:"192.168.1.147", Port: 6969 },
			  RoutingServiceAddress: RoutingServiceAddress }
	server.sendUpdateToRoutingService()
	server.start()
}
