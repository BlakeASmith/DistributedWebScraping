package main

import (
	"encoding/json"
	"log"
	"strconv"
	"time"
	"sync"
	"github.com/etcd-io/etcd/clientv3"
	"net/http"
	"net/url"
	"github.com/temoto/robotstxt"
)

type Job struct {
	Id int
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

// serialize a Service to byte array 
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

// connect to Etcd (Key-Value store)
func initEtcd(config *Config) *clientv3.Client{
	dialTimeout := 5 * time.Second
	// requestTimeout := 5 * time.Second
	cli, err := clientv3.New(clientv3.Config{
		Endpoints:   config.EtcdEndpoints,
		DialTimeout: dialTimeout,
	})
	if err != nil {
		log.Fatal(err)
	}
	return cli
}

func main() {
	// load configuration from environment variables
	config := getConfig()
	log.Println("using config ", config)
	
	//initialize etcd and Kafka variables
	cli := initEtcd(config)
	defer cli.Close()

	kaf := Kafka{Bootstraps: config.Bootstraps}
	producer := kaf.Producer()
	defer producer.Close()
	
	receive, commit := kaf.NonCommittingChannel("services", 1)		//NonCommittingChannel so that we can non-destructively read from topic
	for msg := range receive  {
		service := DeserializeService(msg.Value)
		wg := sync.WaitGroup{}
		jobs := JobChannelFor(service, cli, &wg)
		
		PushJobsToKafka(producer, jobs, config.Delay)
		wg.Wait()
		
		commit <- msg											//commit the read once we are fully done with the work, ensure that nothing gets lost
	}
}

//utilized to get a basic boolean map of subdomains for a given URL of what is OK to crawl
func GetRobotsTxt(url *url.URL) *robotstxt.RobotsData{
	robotsurl := url.Scheme + "://" + url.Host + "/robots.txt"
	// log.Println(robotsurl)
	resp, err := http.Get(robotsurl)
	if err != nil { log.Println("could not get ", robotsurl); return nil }
	defer resp.Body.Close()
	robots, err := robotstxt.FromResponse(resp)
	if err != nil { log.Println("invalid robots.txt", err); return nil }
	return robots
}


func JobChannelFor(service *Service, cli *clientv3.Client, wg *sync.WaitGroup) chan Job {
	urls := make(chan string)
	for _, domain := range service.RootDomains {
		parsed, err := url.Parse(domain)
		if err != nil { log.Println("illegal url"); continue }
		robots := GetRobotsTxt(parsed)
		go crawl(parsed, urls, service, cli, 0, wg, robots)
	}
	// TODO: add job size to service definition
	jobs := makeJobChannel(urls, 10, service.Plugins, service.Name)
	return jobs
}
