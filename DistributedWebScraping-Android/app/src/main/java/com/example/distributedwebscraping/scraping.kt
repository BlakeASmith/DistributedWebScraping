import kotlinx.coroutines.flow.*
import org.jsoup.nodes.Document
import org.jsoup.Jsoup
import java.net.URL

object JsoupHttp: HttpRequestHandler<Document>{
    override suspend fun <R> get(url: String, action: suspend (Document) -> R): R? = kotlin.runCatching {
        action(Jsoup.connect(
                if ("https" in url) url
                else url.replace("http", "https")
        ).get())
    }.getOrDefault(null)
}

/**
 * perform an operation on all of the HTML pages present
 * at the given URLs. The HTML is parsed using JSoup
 *
 * @param urls: a list of urls to process
 * @param nParallel: the number of HTTP requests to execute at once
 * @param action: a mapping from a Document to a JSON string
 *
 * @return a sequence of JSON strings. The HTTP requests are executed in parallel
 * but are not awaited until requested from the sequence
 * */

fun <R> Flow<URL>.scrape(action: suspend (Document) -> R): Flow<Pair<URL,R>> =
        this.map { it to JsoupHttp.get(it.toString(), action) }.filterNot { it.second == null }.map { it.first to it.second!! }

/**
 * count the occurrences of each word in the Document
 */
fun wc(html: Document)= html.allElements
        .map { it.text() }
        .flatMap { it.split(" ") }
        .groupBy { it }
        .mapValues { (_ , value) -> value.size }




