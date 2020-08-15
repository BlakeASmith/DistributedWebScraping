package main

import (
	"fmt"
	"hash/fnv"
	"os"
	"strings"
	"time"

	"github.com/confluentinc/confluent-kafka-go/kafka"
)

type Kafka struct {
	Bootstraps []string
}

// Get a Kafka producer
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

// get a Kafka Consumer
// strategy = "earliest" ==> Kafka will send entries from the earliest committed offset
// strategy = "latest" ==> Kafka will send the most recent events first
func (kaf *Kafka) Consumer(groupid string, strategy string, autocommit bool, records int) *kafka.Consumer {
	consumer, err := kafka.NewConsumer(&kafka.ConfigMap{
		"bootstrap.servers":  strings.Join(kaf.Bootstraps, ","),
		"group.id":           groupid,
		"auto.offset.reset":  strategy,
		"enable.auto.commit": autocommit,
	})
	if err != nil {
		panic(err)
	}
	return consumer
}

func consumeAsChannel(consumer *kafka.Consumer) chan kafka.Message {
	channel := make(chan kafka.Message)
	go func() {
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


func (kaf *Kafka) NonCommittingChannel(topic string, records int) (<-chan kafka.Message, chan<- kafka.Message) {
	cons := kaf.Consumer("golang", "earliest", false,  records)
	topicch := ConsumeTopicAsChannel("services", cons)
	commitCh := make(chan kafka.Message)
	go func() {
		for msg := range commitCh{
			cons.CommitMessage(&msg)
		}
	}()
	return topicch, commitCh
}

//func CompletedJobsChannel(kafka *Kafka) chan Job {
	//cons := kafka.Consumer("golang", "earliest", false, )
	//jobChannel := ConsumeTopicAsChannel("completed", cons)
	//deserialized := make(chan Job)
	//go func() {
		//for message := range jobChannel {
			//deserialized <- *DeserializeJob(message.Value)
		//}
	//}()
	//return deserialized
//}

func SelectPartition(job *Job, nPartitions uint32) int32 {
	h := fnv.New32a()
	h.Write([]byte(strings.Join(job.Urls, "")))
	return int32(h.Sum32() % nPartitions)
}

// send urls from the given channel to kafka, then output them into the returned channel
func PushJobsToKafka(producer *kafka.Producer, channel chan Job, delay time.Duration) {
	delivery := make(chan kafka.Event)
	jobtopic := "jobs"
	go func() {
		for it := range channel {
			producer.Produce(
				&kafka.Message{
					TopicPartition: kafka.TopicPartition{Topic: &jobtopic, Partition: kafka.PartitionAny},
					Value:          it.Value(),
				},
				delivery,
			)
			fmt.Println("sent to ", string(jobtopic))
			time.Sleep(delay)
		}
	}()
}

//Qfunc
func PushServicesToKafka(producer *kafka.Producer, channel chan Service) {
	delivery := make(chan kafka.Event)
	go func() {
		for it := range channel {
			jobtopic := "services"
			producer.Produce(
				&kafka.Message{
					TopicPartition: kafka.TopicPartition{Topic: &jobtopic, Partition: kafka.PartitionAny},
					Value:          it.Value(), //serialized to json
				},
				delivery,
			)
			fmt.Println("sent to ", jobtopic)
		}
	}()
}
