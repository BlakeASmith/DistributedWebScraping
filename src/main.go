package main

import "os"

func main() {
	args := os.Args[1:]


	urlchan := make(chan string)
	seen := make(map[string]bool)
	go crawl("https://www.usedvictoria.com", "", urlchan, 30, seen)
	jobs := makeJobChannel(urlchan, 10)

	RoutingServiceAddress :=  "http://blakesmith.pythonanywhere.com"
	server := Server{
		IPAddress: Address{ IP:args[0], Port: 6969 },
		RoutingServiceAddress: RoutingServiceAddress,
		JobChannel:            jobs,
	}
	server.sendUpdateToRoutingService()
	server.start()
}
