package main

import (
	"./proto"
	"net/http"
	"strings"
	"log"
	"github.com/PuerkitoBio/goquery"
)


// get a goquery document from a url
func getDocument(url string) (*goquery.Document, error) {
	response, err := http.Get(url)
	if err != nil { return nil, err }
	defer response.Body.Close()

	document, err := goquery.NewDocumentFromReader(response.Body)
	if err != nil { log.Fatal(err) }
	return document, nil
}

// get all links from a goquery Document
func getLinks(doc *goquery.Document) []string {
	links := make([]string, 0)
	doc.Find("a").Each(func (foo int, elem *goquery.Selection) {
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
		!strings.Contains(url, "www."){
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
func crawl(root string, path string, inputchan chan string, depth int, ht map[string] bool){
	if depth == 0 { return }
	if doc, err := getDocument(root+path); err == nil {
		urls := restrictDomain(root, normalizeUrls(root,
				getLinks(doc)))

		for _, url := range urls {
			if val, ok := ht[url]; !ok || !val {
				inputchan <- url
			}else {
				//log.Println("already seen ", url)
			}
		}

		for _, url := range urls {
			if _, ok := ht[url]; !ok {
				ht[url] = true
				if depth % 26 == 0 {
					go crawl(root, strings.Replace(url, root, "", 1), inputchan, depth - 1, ht)
				}else {
					crawl(root, strings.Replace(url, root, "", 1), inputchan, depth - 1, ht)
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
	go func () {
		for{
			chunk := make([]string, 0, chunksize)
			for i := 0; i < chunksize; i++ {
				chunk = append(chunk, <-urlchan)
			}

			jobIds += 1
			jobChan <- proto.Job{
				Id: jobIds,
				Urls: chunk,
				Type: proto.Job_SCRAPING,
			}
		}
	}()
	return jobChan
}


func nextJob() *proto.Job {
	return &proto.Job{ Urls: []string {
			"https://www.yahoo.com",
			"https://www.amazon.com",
			"https://www.wikipedia.org",
			"http://www.qq.com",
			"http://www.google.co.in",
			"http://www.twitter.com",
			"http://www.live.com",
			"http://www.taobao.com",
			"http://www.bing.com",
			"http://www.instagram.com",
			"http://www.weibo.com",
			"http://www.sina.com.cn",
			"http://www.linkedin.com",
			"http://www.yahoo.co.jp",
			"http://www.msn.com",
			"http://www.vk.com",
			"http://www.google.de",
			"http://www.yandex.ru",
			"http://www.hao123.com",
			"http://www.google.co.uk",
			"http://www.reddit.com",
			"http://www.ebay.com",
			"http://www.google.fr",
			"http://www.t.co",
			"http://www.tmall.com",
			"http://www.google.com.br",
			"http://www.360.cn",
			"http://www.sohu.com",
			"http://www.amazon.co.jp",
			"http://www.pinterest.com",
			"http://www.netflix.com",
			"http://www.google.it",
			"http://www.google.ru",
			"http://www.microsoft.com",
			"http://www.google.es",
			"http://www.wordpress.com",
			"http://www.gmw.cn",
			"http://www.tumblr.com",
			"http://www.paypal.com",
			"http://www.blogspot.com",
			"http://www.imgur.com",
			"http://www.stackoverflow.com",
			"http://www.aliexpress.com",
			"http://www.naver.com",
			"http://www.ok.ru",
			"http://www.apple.com",
			"http://www.github.com",
			"http://www.chinadaily.com.cn",
			"http://www.imdb.com",
			"http://www.google.co.kr",
			"http://www.fc2.com",
			"http://www.jd.com",
			"http://www.blogger.com",
			"http://www.163.com",
			"http://www.google.ca",
			"http://www.whatsapp.com",
			"http://www.amazon.in",
			"http://www.office.com",
			"http://www.tianya.cn",
			"http://www.google.co.id",
			"http://www.youku.com",
			"http://www.rakuten.co.jp",
			"http://www.craigslist.org",
			"http://www.amazon.de",
			"http://www.nicovideo.jp",
			"http://www.google.pl",
			"http://www.soso.com",
			"http://www.bilibili.com",
			"http://www.dropbox.com",
			"http://www.xinhuanet.com",
			"http://www.outbrain.com",
			"http://www.pixnet.net",
			"http://www.alibaba.com",
			"http://www.alipay.com",
			"http://www.microsoftonline.com",
			"http://www.booking.com",
			"http://www.googleusercontent.com",
			"http://www.google.com.au",
			"http://www.popads.net",
			"http://www.cntv.cn",
			"http://www.zhihu.com",
			"http://www.amazon.co.uk",
			"http://www.diply.com",
			"http://www.coccoc.com",
			"http://www.cnn.com",
			"http://www.bbc.co.uk",
			"http://www.twitch.tv",
			"http://www.wikia.com",
			"http://www.google.co.th",
			"http://www.go.com",
			"http://www.google.com.ph",
			"http://www.doubleclick.net",
			"http://www.onet.pl",
			"http://www.googleadservices.com",
			"http://www.accuweather.com",
		}, Id: 1, Type: proto.Job_SCRAPING }
}
