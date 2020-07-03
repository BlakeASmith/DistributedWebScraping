
interface HttpRequestHandler<RESPONSE> {
    fun <R> get(url: String, action: (RESPONSE) -> R): R
}

