import kotlinx.coroutines.Deferred

interface HttpRequestHandler<RESPONSE> {
    fun <R> get(url: String, action: (RESPONSE) -> R): Deferred<R>
}

