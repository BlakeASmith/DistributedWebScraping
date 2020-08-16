package main

import (
	"context"
	"log"
	"net/http"
	"strings"
	"sync"
	"time"
	"regexp"
	"net/url"

	"github.com/PuerkitoBio/goquery"
	"github.com/etcd-io/etcd/clientv3"
	"github.com/temoto/robotstxt"
)

// fetch HTML from the given url and parse it into a goquery document
func getDocument(url string) (*goquery.Document, error) {
	response, err := http.Get(url)
	if err != nil {
		return nil, err
	}
	defer response.Body.Close()

	document, err := goquery.NewDocumentFromReader(response.Body)
	if err != nil {
		log.Fatal(err)
	}
	return document, nil
}

// extract all hrefs from a goquery document and return as a slice
func getLinks(doc *goquery.Document) []string {
	links := make([]string, 0)
	doc.Find("a").Each(func(n int, elem *goquery.Selection) {
		href, exists := elem.Attr("href")
		if exists {
			links = append(links, href)
		}
	})
	return links
}

// convert any relative links to absolute URLs
func normalizeUrls(baseuri string, urls []string) []string {
	for i, url := range urls {
		if !strings.Contains(url, baseuri)  {
			if strings.Contains(url, "http"){ continue }
			if strings.HasPrefix(url, "/"){
				urls[i] = baseuri + url
			} else {
				urls[i] = baseuri + "/" + url
			}
		}
	}
	return urls
}

// restricts urls to the given base domain, returns those urls 
// under the same base domain as a slice
func restrictDomain(baseuri string, urls []string) []string {
	restricted := make([]string, 0)
	for _, url := range urls {
		if strings.Contains(url, baseuri) {
			restricted = append(restricted, url)
		}
	}
	return restricted
}

func ShouldIgnore(root string, url string, filters []string) bool {
	path := strings.TrimPrefix(url, root)
	for _, ignore := range filters {
		if m, err := regexp.MatchString(ignore, path); err != nil || m {
			return true
		}
	}
	return false
}

//remove relative links that look unique but aren't
func IsLegal(url string) bool {
	if strings.Contains(url, "../") {
		return false
	}
	//if m, err := regexp.Match(".*//.*//.*", []byte(url)); err != nil || m {
		//return false
	//}
	return true
}

func FilterIllegalAndSeenUrls(root string, urls []string, service *Service,  cli *clientv3.Client, robots *robotstxt.RobotsData) []string {
	newUrls := make([]string, 0)
	for _, url := range urls {
		if in(getValue(url, cli), service.Name) { continue } // url has already been discovered
		if ShouldIgnore(root, url, service.Filters) { continue } // url contains illegal path
		if !IsLegal(url) { continue }
		if robots != nil {
			allowed := robots.TestAgent(url, "csc462-Bot")
			if !allowed { log.Println(url, "dissalowed in robots.txt"); continue }
		}
		newUrls = append(newUrls, url)
	}
	return newUrls
}

// recursivley crawl the urls given in a Service to a set depth level, 
// then create new Service definitions from the urls discovered at that depth
func crawl(startUrl *url.URL, inputchan chan string,
	service *Service, cli *clientv3.Client, depth int, wg *sync.WaitGroup, robots *robotstxt.RobotsData) {

	wg.Add(1)
	defer wg.Done()

	log.Println("Starting crawl at depth ", depth, "at ", startUrl.String())

	// try to get the document at the given url
	doc, err := getDocument(startUrl.String())
	if err != nil {
		log.Println("could not get document from ", startUrl.String())
		return
	}
	log.Println("retreived document from ", startUrl.String())

	normalizedUrls := normalizeUrls(startUrl.Scheme + "://" + startUrl.Host, getLinks(doc))
	urls := restrictDomain(startUrl.Scheme + "://" + startUrl.Host, normalizedUrls)
	newUrls := FilterIllegalAndSeenUrls(startUrl.Scheme + "://" + startUrl.Host, urls, service, cli, robots)
	log.Println("retreived and filtered links from ", startUrl.String())

	if len(newUrls) == 0 { print("returning, no new Urls on this page"); return }


	log.Println("sending new urls ", newUrls)
	for _, url := range newUrls {
		log.Println("discovered ", url)
		inputchan <- url // send discovered url to be grouped into a job
		makeKey(url, service.Name, cli) // mark url as seen for this service
	}

	//TODO: add target depth to service definition
	if depth == 1 {
		// create and send new services so that work can be distributed amungst 
		// other producer nodes
		serviceChan := make(chan Service)
		sendNewService(serviceChan)
		groupSize := 5 // TODO: make group size configurable
		grp := make([]string, 0, groupSize)
		for i, url := range newUrls {
		        log.Println("creating service for ", url)
			grp = append(grp, url)
			if i % groupSize == 0 {
				serviceChan <- Service{
					Name:        service.Name,
					RootDomains: grp,
					Filters:     service.Filters,
					Plugins:     service.Plugins,
				}
				grp = make([]string, 0, groupSize)
			}
		}
		return
	}

	// recursive step
	for _, _url := range newUrls {
		log.Println("recursing on ", _url)
		parsed, err :=  url.Parse(_url)
		if err != nil {
			log.Println(_url, " is not a valid URL")
			continue
		}
		crawl(parsed, inputchan, service, cli, depth+1, wg, robots)
	}
}

func makeJobChannel(urlchan chan string, chunksize int, plugins []string, service string) chan Job {
	jobChan := make(chan Job)
	jobIds := 0
	go func() {
		for {
			chunk := make([]string, 0, chunksize)
			for i := 0; i < chunksize; i++ {
				chunk = append(chunk, <-urlchan)
			}
			jobIds += 1
			jobChan <- Job{
				Id:      jobIds,
				Urls:    chunk,
				Plugins: plugins,
				Service: service,
			}
		}
	}()
	return jobChan
}

func makeKey(url string, name string, cli *clientv3.Client) { //context?
	cur_val := getValue(url, cli)
	cur_val = append(cur_val, name)
	cur_val_str := arr_to_str(cur_val)
	_, err := cli.Put(context.TODO(), url, cur_val_str) 
	if err != nil {
		log.Fatal(err)
	}
}

func getValue(key string, cli *clientv3.Client) []string {
	requestTimeout := 5 * time.Second
	ctx, cancel := context.WithTimeout(context.Background(), requestTimeout)

	resp, err := cli.Get(ctx, key)
	cancel()
	if err != nil {
		log.Fatal(err)
	}

	val := resp.Kvs
	if val == nil {
		return []string{}
	} else {
		return str_to_arr(string(val[0].Value))
	}
}

func sendNewService(serviceChan chan Service) {
	config := getConfig()
	kaf := Kafka{Bootstraps: config.Bootstraps}
	producer := kaf.Producer()
	PushServicesToKafka(producer, serviceChan)
}

func in (vals []string, val string) bool{
	for _,a := range vals {
		if a==val{
			return true
		}
	}
	return false
}

func arr_to_str(vals []string) string{
	return strings.Join(vals, " ")
}

func str_to_arr(val string) []string{
	return strings.Split(val, " ")
}
