package main

import (
	"log"
	"os"
	"strconv"
	"strings"
	"time"
)

type Config struct {
	Bootstraps []string
	EtcdEndpoints []string
	Delay      time.Duration
	Debug      bool
}

func getConfig() *Config {
	var config Config
	var boots string
	var etcds string
	var delay string
	var debug bool

	if _boots, exists := os.LookupEnv("WEBSCRAPER_BOOTSTRAPS"); exists {
		boots = _boots
	} else {
		boots = "localhost:9092"
		log.Println("WARN: environment variable WEBSCRAPER_BOOTSTRAPS not set, using localhost")
	}

	if _delay, exists := os.LookupEnv("DELAY_BETWEEN_JOBS"); exists {
		delay = _delay
	} else {
		delay = "100"
	}

	if _debug, exists := os.LookupEnv("WEBSCRAPER_DEBUG"); exists {
		debug = (_debug == "true")
	} else {
		debug = true
	}

	if _etcds, exists := os.LookupEnv("WEBSCRAPER_ETCD_SERVERS"); exists {
		etcds = _etcds
	} else {
		etcds = "localhost:2379"
		log.Println("WARN: environment variable WEBSCRAPER_ETCD_SERVERS not set, using localhost")
	}

	delayAsInt, err := strconv.Atoi(delay)
	if err != nil {
		panic(err)
	}

	config = Config{
		Bootstraps: strings.Split(boots, ","),
		Delay:      time.Duration(delayAsInt) * time.Millisecond,
		Debug:      debug,
		EtcdEndpoints: strings.Split(etcds, ","),
	}
	return &config
}
