package services

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

    fun <R> with(key: K, operation: V.() -> R ): R = runBlocking{
        operation(getAsync(key).await())
    }

    suspend operator fun <R> invoke(key: K, operation: suspend (V) -> R) = withAsync(key, operation).await()
}

@ExperimentalCoroutinesApi
@FlowPreview
fun <T, V> Flow<Pair<T, V>>.resolver() = FlowResolver(this) { it }

