package main

import "os"
import "encoding/json"
import "io/ioutil"
import "log"


type Config struct {
	Bootstraps []string
}

func getConfig() *Config {
	var config Config
	if configPath, exists := os.LookupEnv("WEBSCRAPER_CONFIG"); exists {
		jsonfile, err := os.Open(configPath)
		if err != nil { log.Fatal(err)}

		bytes, _ := ioutil.ReadAll(jsonfile)
		json.Unmarshal(bytes, &config)
	}else {
		log.Fatal("environment varliable WEBSCRAPER_CONFIG is not set")
	}

	return &config
}
