package main

import (
	"log"
	"net/http"
	"strings"
	"sync"

	"github.com/PuerkitoBio/goquery"
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
func crawl(root string, path string, inputchan chan string, depth int, ht map[string]bool, ignores []string, plugins []string, name string) {
	log.Println("Starting crawl at depth ", depth, "at ", root, path)
	if doc, err := getDocument(root + path); err == nil {
		urls := restrictDomain(root, normalizeUrls(root,
			getLinks(doc)))
		
		if depth == 6 {		//TODO depth value (Tune somehow, time process vs latency??)
			serviceChan := make(chan Service, 50)
			for _, url := range urls{
				log.Println(url)
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
			if val, ok := ht[url]; !ok || !val {
				shouldIgnore := false
				for _, ignore := range ignores {
					if strings.Contains(url, ignore){
						shouldIgnore = true
					}
				}
				if !shouldIgnore {
					log.Println("pushing ", url)
					inputchan <- url
				}
			} else {
				//log.Println("already seen ", url)
			}
		}

		for _, url := range urls {
			if _, ok := ht[url]; !ok {
				ht[url] = true
				crawl(root, strings.Replace(url, root, "", 1), inputchan, depth+1, ht, ignores, plugins, name)
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


func sendNewService(serviceChan chan Service){
	config := getConfig()
	kaf := Kafka{ Bootstraps:config.Bootstraps }
	producer := kaf.Producer()
	PushServicesToKafka(producer, serviceChan)
}
