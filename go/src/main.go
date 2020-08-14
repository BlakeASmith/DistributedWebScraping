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



	dialTimeout := 5 * time.Second
	// requestTimeout := 5 * time.Second
	cli, err := clientv3.New(clientv3.Config{
		Endpoints:   config.EtcdEndpoints,
		DialTimeout: dialTimeout,
	})
	if err != nil {
		log.Fatal(err)
	}
	defer cli.Close()


	kaf := Kafka{Bootstraps: config.Bootstraps}
	producer := kaf.Producer()
	defer producer.Close()

	receive, commit := kaf.NonCommittingChannel("services", 1)
	for msg := range receive  {
		service := DeserializeService(msg.Value)
		print(service)
		jobs := JobChannelFor(service, *cli)
		PushJobsToKafka(producer, jobs, config.Delay)
		print("committed service")
		commit <- msg
	}
}

func JobChannelFor(service *Service, cli clientv3.Client) chan Job {
	urls := make(chan string)
	for _, domain := range service.RootDomains {
		log.Println("starting crawl on", domain)
		go crawl(domain, "", urls, -1, service.Filters, service.Plugins, service.Name, cli)
	}
	jobs := makeJobChannel(urls, 10, service.Plugins, service.Name)
	return jobs
}
