package main

import "os"
import "log"

func main() {
	ip := os.Args[1]
	plugins := os.Args[2:]
	if len(plugins) == 0{
		plugins = append(plugins, "wordcount.jar")
	}

	kaf := Kafka{ Bootstraps:[]string{ip+":9092"}, ClientID:"go" }
	urlchan := make(chan string)
	seen := make(map[string]bool)

	go crawl("https://www.usedvictoria.com", "", urlchan, 35, seen)

	kafChan := kaf.pushToTopic(urlchan, "url", plugins[0])

	for url := range kafChan {
		log.Println(url + " sent to kafka")
	}


	//jobs := makeJobChannel(urlchan, 5, plugins)

	//RoutingServiceAddress := "http://blakesmith.pythonanywhere.com"
	//server := Server{
		//IPAddress:             Address{IP: ip, Port: 6969},
		//RoutingServiceAddress: RoutingServiceAddress,
		//JobChannel:            jobs,
	//}
	//server.sendUpdateToRoutingService()
	//server.start()
}

