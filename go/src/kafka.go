package main

import (
	"github.com/confluentinc/confluent-kafka-go/kafka"
	"strings"
)

type Kafka struct {
	Bootstraps []string
	ClientID string
}

func (kaf *Kafka) Producer() *kafka.Producer {
	producer, err := kafka.NewProducer(
		&kafka.ConfigMap{
			"bootstrap.servers": strings.Join(kaf.Bootstraps, ","),
			"client.id": kaf.ClientID,
		},
	)

	if err != nil {
		panic(err)
	}
	return producer
}

// send urls from the given channel to kafka, then output them into the returned channel
func (kaf *Kafka) pushToTopic(channel chan string, topic string, plugin string) chan string {
	prod := kaf.Producer()
	newChan := make(chan string)
	delivery := make(chan kafka.Event)
	go func () {
		for it := range channel {
			prod.Produce(
				&kafka.Message{
					TopicPartition: kafka.TopicPartition{Topic: &topic, Partition: kafka.PartitionAny},
					Key: []byte(plugin),
					Value: []byte(it),
				},
				delivery,
			)
		}
	}()

	go func () {
		for event := range delivery {
			m := event.(*kafka.Message)
			newChan <- string(m.Value)
		}
	}()

	return newChan
}

