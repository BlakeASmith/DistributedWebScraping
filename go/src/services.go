package main

import "encoding/json"

type Service struct {
	Name string
	RootDomains []string
	Filters []string
	Plugins []string
	
	HashTable map[string]bool		//todo replace with distributed 
	//delay int
}

func DeserializeService(bytes []byte) *Service {
	var service Service
	json.Unmarshal(bytes, &service)
	return &service
}


