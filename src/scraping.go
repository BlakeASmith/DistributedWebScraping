package main

import (
	"log"
	"net/http"
	"strings"
	"sync"

	"github.com/BlakeASmith/DistributedWebScraping/src/proto"
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
func crawl(root string, path string, inputchan chan string, depth int, ht map[string]bool) {
	if depth == 0 {
		return
	}
	if doc, err := getDocument(root + path); err == nil {
		urls := restrictDomain(root, normalizeUrls(root,
			getLinks(doc)))

		for _, url := range urls {
			if val, ok := ht[url]; !ok || !val {
				//log.Println("pushing ", url)
				inputchan <- url
			} else {
				//log.Println("already seen ", url)
			}
		}

		for _, url := range urls {
			if _, ok := ht[url]; !ok {
				ht[url] = true
				if depth%2 == 0 {
					log.Println(depth)
					crawl(root, strings.Replace(url, root, "", 1), inputchan, depth-1, ht)
				} else {
					crawl(root, strings.Replace(url, root, "", 1), inputchan, depth-1, ht)
				}
			}
		}
	} else {
		log.Println("could not get document from ", root+path)
	}
}

func makeJobChannel(urlchan chan string, chunksize int) chan proto.Job {
	jobChan := make(chan proto.Job)
	var jobIds int32 = 0
	N := 5
	var wg sync.WaitGroup
	go func() {
		for {
			wg.Add(N)
			for i := 0; i <= N; i++ {
				chunk := make([]string, 0, chunksize)
				for i := 0; i < chunksize; i++ {
					chunk = append(chunk, <-urlchan)
				}

				jobIds += 1
				//log.Println("created a new job")
				go func() {
					jobChan <- proto.Job{
						Id:   jobIds,
						Urls: chunk,
						Type: proto.Job_SCRAPING,
					}
					wg.Done()
				}()
			}
			wg.Wait()
		}
	}()
	return jobChan
}
