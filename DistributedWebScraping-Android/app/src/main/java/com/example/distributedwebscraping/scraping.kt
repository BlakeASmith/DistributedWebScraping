import kotlinx.coroutines.Deferred
import org.jsoup.nodes.Document

class JsoupHttpRequestHandler: HttpRequestHandler<Document>{
    override fun <R> get(url: String, action: (Document) -> R): Deferred<R> {
        TODO("Not yet implemented")
    }
}

fun scrape(urls: List<String>, http: HttpRequestHandler<Document>): Sequence<String> = sequence {
    urls.forEach {
        yield(it)
    }
}