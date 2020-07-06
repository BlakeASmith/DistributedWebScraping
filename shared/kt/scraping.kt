import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object JsoupHttp: HttpRequestHandler<Document>{
    override fun <R> get(url: String,error: (Throwable) -> R, action: (Document) -> R) = GlobalScope.async(Dispatchers.IO) {
        val httpsurl = if ("https" in url) url else url.replace("http", "https")
        kotlin.runCatching {  Jsoup.connect(httpsurl).get().let(action) }
            .getOrElse(error)
    }
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
fun scrape(
    urls: Iterable<String>, nParallel: Int = 10,
    action: (Document) -> String
) = urls
    .asSequence()
    .chunked(nParallel)
    .flatMap { list ->
        list.map {
            JsoupHttp.get(it, { err -> err.toString() }, action)
        }.asSequence()
    }
    .map{ runBlocking { it.await() } }

fun links(document: Document) = document.select("a")
    .map { it.attr("href") }

suspend fun crawlPage(root: String, startPath: String = ""): Flow<String> = flowOf(root + startPath)
        .map { JsoupHttp.get(it) { html -> links(html) } }
        .map { it.await() }
        .flatMapMerge { it.asFlow() }
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

