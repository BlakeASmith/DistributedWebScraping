import com.google.gson.Gson
import kotlinx.coroutines.flow.*
import org.jsoup.nodes.Document
import org.jsoup.Jsoup
import java.net.URL

object JsoupHttp: HttpRequestHandler<Document>{
    override suspend fun <R> get(url: String,error: (Throwable) -> R, action: suspend (Document) -> R) = kotlin.runCatching {
        action(Jsoup.connect(
                if ("https" in url) url
                else url.replace("http", "https")
        ).get())
    }.getOrElse(error)
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

fun Flow<URL>.scrape(action: suspend (Document) -> String) = this.map { JsoupHttp.get(it.toString(), { err -> err.toString() }, action) }

fun links(document: Document) = document.select("a")
    .map { it.attr("href") }.asFlow()

suspend fun crawlPage(root: String, startPath: String = ""): Flow<String> = flowOf(root + startPath)
        .map { JsoupHttp.get(it) { html -> links(html) } }.flattenMerge()
        .filter { "http" in it && root !in it }
        .map { if (root in it) it else root+it }

suspend fun crawl(root: String, startPath: String, depth: Int = 3, curDepth: Int = 0) = flow<String> {
        val pages = crawlPage(root, startPath)
        pages.onEach {
            emit(it)
            val nextSet = crawlPage(root, it.replace(root, ""))
            emitAll(nextSet)
        }.toList()
}




/**
 * count the occurrences of each word in the Document
 */
fun wc(html: Document)= html.allElements
    .map { it.text() }
    .flatMap { it.split(" ") }
    .groupBy { it }
    .mapValues { (_ , value) -> value.size }
    .let { Gson().toJson(it) }


