package main

import "log"
import "encoding/json"
import "strconv"

type KeyValueSerializer interface {
	Key() []byte
	Value() []byte
}

type Job struct {
	Id int
	Urls []string
	Plugins []string
	Service string
}

func (j Job) Key() []byte {
	return []byte(strconv.Itoa(j.Id))
}

func (j Job) Value() []byte {
	json, err := json.Marshal(j)
	if err != nil {
		panic(err)
	}
	return json
}

func main() {
	config := getConfig()
	log.Println("using config ", config)


	kaf := Kafka{ Bootstraps:config.Bootstraps }
	consumer := kaf.Consumer("golang", "earliest", false)
	serviceChannel := ConsumeTopicAsChannel("services", consumer)
	producer := kaf.Producer()

	for message := range serviceChannel{
		log.Println("received service " + string(message.Key))
		service := DeserializeService(message.Value)
		go PushToTopic(producer, "jobs" , JobChannelFor(service))
	}
}

func JobChannelFor(service *Service) chan KeyValueSerializer {
	urls := make(chan string)
	seen := make(map[string] bool)
	for _, domain := range service.RootDomains{
		log.Println("starting crawl on", domain)
		go crawl(domain, "", urls, -1, seen, service.Filters)
	}
	serializerChannel := make(chan KeyValueSerializer)
	jobs := makeJobChannel(urls, 10, service.Plugins, service.Name)

	go func () {
		for job := range jobs {
			serializerChannel <- interface{}(job).(KeyValueSerializer)
		}
	}()
	return serializerChannel
}

