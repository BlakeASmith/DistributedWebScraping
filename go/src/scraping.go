package main

import (
	"log"
	"net/http"
	"strings"
	"sync"
	"context"
	"time"
	
	"github.com/PuerkitoBio/goquery"
	"github.com/etcd-io/etcd/clientv3"
)

// get a goquery document from a url
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

// get all links from a goquery Document
func getLinks(doc *goquery.Document) []string {
	links := make([]string, 0)
	doc.Find("a").Each(func(foo int, elem *goquery.Selection) {
		href, exists := elem.Attr("href")
		if exists {
			links = append(links, href)
		}
	})
	return links
}

// add the baseuri to any urls which do not contain it
// required for interpreting relative urls
func normalizeUrls(baseuri string, urls []string) []string {
	for i, url := range urls {
		if !strings.Contains(url, "http") &&
			!strings.Contains(url, baseuri) &&
			!strings.Contains(url, "www.") {
			urls[i] = baseuri + url
		}
	}
	return urls
}

func restrictDomain(baseuri string, urls []string) []string {
	restricted := make([]string, 0)
	for _, url := range urls {
		if strings.Contains(url, baseuri) {
			restricted = append(restricted, url)
		}
	}
	return restricted
}

// crawl all pages without leaving a set domain. Only return urls which have not yet been seen
func crawl(root string, path string, inputchan chan string, depth int, ht map[string]bool, ignores []string, plugins []string, name string, cli clientv3.Client) {
	log.Println("Starting crawl at depth ", depth, "at ", root, path)
	if doc, err := getDocument(root + path); err == nil {
		urls := restrictDomain(root, normalizeUrls(root,
			getLinks(doc)))
		
		if depth == 6 {		//TODO depth value (Tune somehow, time process vs latency??)
			serviceChan := make(chan Service, 50)
			for _, url := range urls{
				// log.Println(url)
				serviceChan <- Service{
					Name: name,
					RootDomains: []string{url},
					Filters: ignores,
					Plugins: plugins,
					HashTable: ht,
				}
			
			}
			sendNewService(serviceChan)
			return
		}
		
		for _, url := range urls {
			
			if getValue(url, cli) != name{
				shouldIgnore := false
				for _, ignore := range ignores {
					if strings.Contains(url, ignore){
						shouldIgnore = true
					}
				}
				if !shouldIgnore {
					// log.Println("pushing ", url)
					inputchan <- url
					makeKey(url, name, cli)
					log.Println(getValue(url, cli))
					
				}
			} else {
				// log.Println("already seen ", url)
			}
		}

		for _, url := range urls {
			if _, ok := ht[url]; !ok {
				ht[url] = true
				// makeKey(url, name, cli)	//TODO add to list instead of overwriting
				crawl(root, strings.Replace(url, root, "", 1), inputchan, depth+1, ht, ignores, plugins, name, cli)
			}
		}
	} else {
		log.Println("could not get document from ", root+path)
	}
}

func makeJobChannel(urlchan chan string, chunksize int, plugins []string, service string) chan Job {
	jobChan := make(chan Job)
	var jobIds int = 0
	N := 5
	var wg sync.WaitGroup
	go func() {
		for {
			wg.Add(N)
			for i:= 0; i < N; i++ {
				chunk := make([]string, 0, chunksize)
				for i := 0; i < chunksize; i++ {
					chunk = append(chunk, <-urlchan)
				}

				jobIds += 1
				go func () {
					jobChan <- Job{
						Id:   jobIds,
						Urls: chunk,
						Plugins: plugins,
						Service: service,
					}
					wg.Done()
				}()
			}
			wg.Wait()
		}
	}()
	return jobChan
}

func makeKey(url string, name string, cli clientv3.Client){		//context?
	_, err := cli.Put(context.TODO(), url, name) //20 GOTO 10
	if err != nil {log.Fatal(err)}
}

func getValue(key string, cli clientv3.Client) string{
	requestTimeout := 5 * time.Second
	ctx, cancel := context.WithTimeout(context.Background(), requestTimeout)

	resp, err := cli.Get(ctx, key)
	cancel()
	if err != nil {log.Fatal(err)}
	// for _, ev := range resp.Kvs {
		// log.Printf("%s : %s\n", ev.Key, ev.Value)
	// }
	val := resp.Kvs
	if val==nil{
		return ""
	} else {
		return string(val[0].Value)
	}
}


func sendNewService(serviceChan chan Service){
	config := getConfig()
	kaf := Kafka{ Bootstraps:config.Bootstraps }
	producer := kaf.Producer()
	PushServicesToKafka(producer, serviceChan)
}
