import kotlinx.coroutines.Deferred

interface HttpRequestHandler<RESPONSE> {
    suspend fun <R> get(url: String, action: suspend (RESPONSE) -> R ): R?
}

