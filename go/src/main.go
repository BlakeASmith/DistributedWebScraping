package main

import(
	"log"
	"encoding/json"
	"strconv"
	"github.com/etcd-io/etcd/clientv3"
	"time"
)

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
	if err != nil {panic(err)}
	return json
}

func (s Service) Value() []byte{
	json, err := json.Marshal(s)
	if err != nil {panic(err)}
	return json
}

func DeserializeJob(job []byte) *Job {
	var djob Job
	json.Unmarshal(job, &djob)
	return &djob
}

func main() {

	config := getConfig()
	num_jobs := 50		//todo add to config
	log.Println("using config ", config)

	kaf := Kafka{ Bootstraps:config.Bootstraps }
	
	
	endpoints := []string{"localhost:2379"}
	dialTimeout := 5 * time.Second
	// requestTimeout := 5 * time.Second
	cli, err := clientv3.New(clientv3.Config{
		Endpoints:   endpoints,
		DialTimeout: dialTimeout,
	})
	if err != nil {log.Fatal(err)}
	defer cli.Close()
	

	if (config.Debug){
		test := make(chan string)
		producer := kaf.Producer()
		
		for i := 0; i < num_jobs; i++{
			service := <- ServicesChannel(&kaf)
			log.Println("received service " + service.Name)
			go PushJobsToKafka(producer, JobChannelFor(&service, *cli), config.Delay)
		}
		
		log.Println("closing producer")
		producer.Close()
		for val := range(test){
			log.Println(val)
		}
		
	} else {
		for true{
			producer := kaf.Producer()
			// for service := range ServicesChannel(&kaf){
			for i := 0; i < num_jobs; i++{
				service := <- ServicesChannel(&kaf)
				log.Println("received service " + service.Name)
				go PushJobsToKafka(producer, JobChannelFor(&service, *cli), config.Delay)
			}
			producer.Close()
			log.Println("closing producer")
		}
	}

}

func JobChannelFor(service *Service, cli clientv3.Client) chan Job {
	urls := make(chan string)
	for _, domain := range service.RootDomains{
		log.Println("starting crawl on", domain)
		go crawl(domain, "", urls, -1, service.Filters, service.Plugins, service.Name, cli)
	}
	jobs := makeJobChannel(urls, 10, service.Plugins, service.Name)
	return jobs
}

