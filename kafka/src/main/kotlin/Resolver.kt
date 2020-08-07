package ca.blakeasmith.kkafka.jvm

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach


import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.*

@FlowPreview
@ExperimentalCoroutinesApi
/**
 * expose values in a flow as Deferred
 */
class FlowResolver<K, V, P> (
        flow: Flow<P>,
        private val scope: CoroutineScope = GlobalScope,
        private val map: MutableMap<K, CompletableDeferred<V>> = mutableMapOf(),
        val transform: (P) -> Pair<K, V>
) {
    constructor(channel: ReceiveChannel<P>, scope: CoroutineScope = GlobalScope, transform: (P) -> Pair<K, V>): this(channel.consumeAsFlow(), scope, mutableMapOf(), transform)

    val job = flow
            .onEach { transform(it).let { map.putIfAbsent(it.first, CompletableDeferred()); map[it.first]!!.complete(it.second) } }
            .launchIn(scope)

    fun getAsync(key: K): Deferred<V> = map.apply { putIfAbsent(key, CompletableDeferred()) }[key]!!

    fun get(key: K) = runBlocking {
        getAsync(key).await()
    }

    fun <R> withAsync(key: K, operation : suspend V.() -> R): Deferred<R> = scope.async {
        operation(getAsync(key).await())
    }

    fun <R> withAsync(vararg keys: K, operation : suspend V.() -> R): List<Deferred<R>> = keys.map { withAsync(it, operation) }

    fun <R> with(key: K, operation: V.() -> R ): R = runBlocking{
        operation(getAsync(key).await())
    }

    fun <R> with(vararg keys: K, operation: V.() -> R) = keys.map { with(it, operation) }


    suspend operator fun <R> invoke(key: K, operation: suspend (V) -> R) = withAsync(key, operation).await()
}

@ExperimentalCoroutinesApi
@FlowPreview
fun <T, V> Flow<Pair<T, V>>.resolver() = FlowResolver(this) { it }

