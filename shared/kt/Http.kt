import kotlinx.coroutines.Deferred

interface HttpRequestHandler<RESPONSE> {
    fun <R> get(url: String, error: (Throwable) -> R = { throw it }, action: (RESPONSE) -> R ): Deferred<R>
}

