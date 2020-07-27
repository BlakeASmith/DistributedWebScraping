import com.google.gson.Gson
import org.jsoup.nodes.Document


class WordCount {
    var url: String? = null
    var counts: Map<String, Int>? = null

    override fun toString(): String = "$url\n$counts"
}

/**
 * count the occurrences of each word in the Document
 */
fun wc(html: Document)= html.allElements
        .map { it.text() }
        .flatMap { it.split(" ") }
        .groupBy { it }
        .mapValues { (_ , value) -> value.size }

class WCPlugin(): RoverPlugin {
    val gson: Gson = Gson()
    override fun scrape(doc: Document): String = wc(doc).let { gson.toJson(WordCount().apply {
        url = doc.location()
        counts = it
    }) }
}