package main

func main() {
	urlchan := make(chan string)
	seen := make(map[string]bool)
	go crawl("https://www.usedvictoria.com", "", urlchan, 7, seen)
	jobs := makeJobChannel(urlchan, 10)

	RoutingServiceAddress := Address{IP: "0.0.0.0", Port: 5000}
	server := Server{
		IPAddress:             Address{IP: "0.0.0.0", Port: 6969},
		RoutingServiceAddress: RoutingServiceAddress,
		JobChannel:            jobs,
	}
	server.sendUpdateToRoutingService()
	server.start()
}
