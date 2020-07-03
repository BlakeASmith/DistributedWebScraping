
interface HttpRequestHandler<RESPONSE> {
    suspend fun <R> get(url: String, action: (RESPONSE) -> R): R
}

