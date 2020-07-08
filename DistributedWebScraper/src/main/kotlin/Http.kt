import kotlinx.coroutines.Deferred

interface HttpRequestHandler<RESPONSE> {
    suspend fun <R> get(url: String, error: (Throwable) -> R = { throw it }, action: suspend (RESPONSE) -> R ): R
}

