package main

import (
	"strings"
	"fmt"
	"os"
	"github.com/confluentinc/confluent-kafka-go/kafka"
)

type Kafka struct {
	Bootstraps []string
}

func (kaf *Kafka) Producer() *kafka.Producer {
	producer, err := kafka.NewProducer(
		&kafka.ConfigMap{
			"bootstrap.servers": strings.Join(kaf.Bootstraps, ","),
		},
	)

	if err != nil {
		panic(err)
	}
	return producer
}

func (kaf *Kafka) Consumer(groupid string, strategy string, autocommit bool) *kafka.Consumer {
	consumer, err := kafka.NewConsumer(&kafka.ConfigMap{
		"bootstrap.servers": strings.Join(kaf.Bootstraps, ","),
		"group.id":          groupid,
		"auto.offset.reset": strategy,
		"enable.auto.commit": autocommit,
	})
	if err != nil {
		panic(err)
	}
	return consumer
}


func consumeAsChannel(consumer *kafka.Consumer) chan kafka.Message {
	channel := make(chan kafka.Message)
	go func () {
		for {
			ev := consumer.Poll(100)
			if ev == nil {
				continue
			}
			switch e := ev.(type) {
				case *kafka.Message:
					channel <- *e
				case kafka.Error:
					fmt.Fprintf(os.Stderr, "%% Error: %v: %v\n", e.Code(), e)
				default:
					fmt.Printf("Ignored %v\n", e)
			}
		}
	}()
	return channel
}

func ConsumeTopicAsChannel(topic string, consumer *kafka.Consumer) chan kafka.Message {
	consumer.SubscribeTopics([]string{topic}, nil)
	return consumeAsChannel(consumer)
}


// send urls from the given channel to kafka, then output them into the returned channel
func PushToTopic(producer *kafka.Producer, topic string, channel chan KeyValueSerializer) {

	delivery := make(chan kafka.Event)
	go func () {
		for it := range channel {
			producer.Produce(
				&kafka.Message{
					TopicPartition: kafka.TopicPartition{Topic: &topic, Partition: kafka.PartitionAny},
					Key: it.Key(),
					Value: it.Value(),
				},
				delivery,
			)
			fmt.Println("sent ", string(it.Key()), " to ", topic)
		}
	}()

	//go func () {
		//for e := range delivery {
			//switch e := e.(type) {
				//case *kafka.Message:
					//fmt.Println("delivered Job ")
				//case kafka.Error:
					//fmt.Fprintf(os.Stderr, "%% Error: %v: %v\n", e.Code(), e)
				//default:
					//fmt.Printf("Ignored %v\n", e)
			//}
		//}
	//}()
}

