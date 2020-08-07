package main

import "log"
import "encoding/json"
import "strconv"

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

func (s Service) Value() []byte{
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

	kaf := Kafka{ Bootstraps:config.Bootstraps }
	producer := kaf.Producer()

	if (config.Debug){
		test := make(chan string)
		test2 := make (map[string] bool)
		log.Println("Sanity")
		go crawl("http://usedvictoria.com", "", test, -1, test2, []string{""}, []string{""}, "usedvic")
		for val := range(test){
			log.Println(val)
		}
	} else {
		for service := range ServicesChannel(&kaf){
			log.Println("received service " + service.Name)
			go PushJobsToKafka(producer, JobChannelFor(&service), config.Delay)
		}
	}

}

func JobChannelFor(service *Service) chan Job {
	urls := make(chan string)
	seen := make(map[string] bool)
	for _, domain := range service.RootDomains{
		log.Println("starting crawl on", domain)
		go crawl(domain, "", urls, -1, seen, service.Filters, service.Plugins, service.Name)
	}
	jobs := makeJobChannel(urls, 10, service.Plugins, service.Name)
	return jobs
}

