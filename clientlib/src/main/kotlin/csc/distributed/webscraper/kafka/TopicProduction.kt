package csc.distributed.webscraper.kafka

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import org.apache.kafka.clients.producer.*


    @FlowPreview
    @ExperimentalCoroutinesApi
    /**
     * Produce to csc.distributed.webscraper.kafka.Kafka topic via a channel
     */
    class TopicProduction<K, V>(
            topic: Topic<K, V>,
            producerConfig: ProducerConfig,
            context: CoroutineScope = GlobalScope,
            private val channel: BroadcastChannel<Pair<K, V>> = BroadcastChannel(Channel.BUFFERED),
            private val producer: Producer<K, V> = KafkaProducer(producerConfig.originals().apply {
                put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, topic.keySerde.serializer()::class.java)
                put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, topic.valueSerde.serializer()::class.java)
            })
    ): SendChannel<Pair<K, V>> by channel{

        val metadata: Channel<RecordMetadata> = Channel(Channel.BUFFERED)

        private val job = channel.asFlow()
                .onEach { producer.send(ProducerRecord(topic.name, it.first, it.second)) { record, exception ->
                    metadata.sendBlocking(record)
                } }
                .onCompletion { producer.close() }
                .launchIn(context)

        suspend fun sendConfirm(element: Pair<K,V>): RecordMetadata {
            send(element)
            return metadata.receive()
        }

        fun observeAsFlow() = channel.asFlow()
        fun observe()  = channel.openSubscription()

        override fun close(cause: Throwable?): Boolean {
            job.cancel(CancellationException("channel closed", cause))
            runBlocking { job.join() }
            metadata.close()
            return channel.close(cause)
        }

        suspend fun closeJoin(cause: Throwable? = null): Boolean{
            job.cancelAndJoin()
            metadata.close()
            return channel.close()
        }
    }
