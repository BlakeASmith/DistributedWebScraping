package main

import (
	"encoding/json"
	"log"
	"strconv"
	"time"

	"github.com/etcd-io/etcd/clientv3"
)

type Job struct {
	Id      int
	Urls    []string
	Plugins []string
	Service string
}

// serialize job Id to a byte array
func (j Job) Key() []byte {
	return []byte(strconv.Itoa(j.Id))
}

// serialize Job to a byte array (JSON)
func (j Job) Value() []byte {
	json, err := json.Marshal(j)
	if err != nil {
		panic(err)
	}
	return json
}

func (s Service) Value() []byte {
	json, err := json.Marshal(s)
	if err != nil {
		panic(err)
	}
	return json
}

func DeserializeJob(job []byte) *Job {
	var djob Job
	json.Unmarshal(job, &djob)
	return &djob
}

func main() {
	config := getConfig()
	log.Println("using config ", config)

	kaf := Kafka{Bootstraps: config.Bootstraps}
	producer := kaf.Producer()

	endpoints := []string{"localhost:2379"}
	dialTimeout := 5 * time.Second
	// requestTimeout := 5 * time.Second
	cli, err := clientv3.New(clientv3.Config{
		Endpoints:   endpoints,
		DialTimeout: dialTimeout,
	})
	if err != nil {
		log.Fatal(err)
	}
	defer cli.Close()

	if config.Debug {
		test := make(chan string)
		test2 := make(map[string]bool)
		log.Println("Sanity")
		go crawl("http://stackoverflow.com/", "", test, -1, test2, []string{}, []string{}, "usedvic", *cli)
		for val := range test {
			// log.Println(val)
			val = val
		}
	} else {
		for service := range ServicesChannel(&kaf) {
			log.Println("received service " + service.Name)
			go PushJobsToKafka(producer, JobChannelFor(&service, *cli), config.Delay)
		}
	}

}

func JobChannelFor(service *Service, cli clientv3.Client) chan Job {
	urls := make(chan string)
	seen := make(map[string]bool)
	for _, domain := range service.RootDomains {
		log.Println("starting crawl on", domain)
		go crawl(domain, "", urls, -1, seen, service.Filters, service.Plugins, service.Name, cli)
	}
	jobs := makeJobChannel(urls, 10, service.Plugins, service.Name)
	return jobs
}
