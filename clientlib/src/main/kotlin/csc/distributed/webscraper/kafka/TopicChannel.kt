package csc.distributed.webscraper.kafka

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import org.apache.kafka.common.serialization.Serde
import java.time.Duration

@FlowPreview
@ExperimentalCoroutinesApi
class TopicChannel<K, V>(
        consumerChannel: ConsumerChannel<K,V>,
        producerChannel: ProducerChannel<K,V>
): SendChannel<Pair<K,V>> by producerChannel, ReceiveChannel<Pair<K,V>> by consumerChannel{
    constructor(
            topic: String,
            clientIds: Pair<String, String>,
            bootstraps: List<String>,
            autocommit: Boolean = true,
            keySerde: Serde<K>,
            valueSerde: Serde<V>,
            context: CoroutineScope = GlobalScope,
            consumerPollingRate: Duration = Duration.ofMillis(100)
    ): this(
            ConsumerChannel(topic, clientIds.first to null, bootstraps,
                    keySerde.deserializer()::class.java, valueSerde.deserializer()::class.java,
                    autocommit, consumerPollingRate, context),
            ProducerChannel(topic, clientIds.second, bootstraps, keySerde.serializer(), valueSerde.serializer(), context)
    )
}
