package main

import "log"
import "encoding/json"
import "strconv"
import "time"

type Job struct {
	Id int
	Urls []string
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

func DeserializeJob(job []byte) *Job {
	var djob Job
	json.Unmarshal(job, &djob)
	return &djob
}

func main() {
	config := getConfig()
	log.Println("using config ", config)

	kaf := Kafka{ Bootstraps:config.Bootstraps }

	producer := kaf.Producer()

	for service := range ServicesChannel(&kaf){
		log.Println("received service " + service.Name)
		go PushJobsToKafka(producer, JobChannelFor(&service), 400 * time.Millisecond)
	}
}

func JobChannelFor(service *Service) chan Job {
	urls := make(chan string)
	seen := make(map[string] bool)
	for _, domain := range service.RootDomains{
		log.Println("starting crawl on", domain)
		go crawl(domain, "", urls, -1, seen, service.Filters)
	}
	jobs := makeJobChannel(urls, 10, service.Plugins, service.Name)
	return jobs
}

