package csc.distributed.webscraper.kafka

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*


@ExperimentalCoroutinesApi
@FlowPreview
fun <K, V> Flow<Pair<K, V>>.produceTo(producer: Production<K, V>, closeProducer: Boolean = true) =
        this.onEach { producer.sendBlocking(it) }.let {
            if (closeProducer) it.onCompletion { producer.close() }
            else it
        }

@ExperimentalCoroutinesApi
@FlowPreview
fun <K, V> Flow<Pair<K, V>>.produceWithConfirmation(producer: Production<K, V>, closeProducer: Boolean = true) =
        this.map { it to producer.sendConfirm(it) }.let {
            if (closeProducer) it.onCompletion { producer.close() }
            else it
        }

