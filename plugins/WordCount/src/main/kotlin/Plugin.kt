import com.google.gson.GsonBuilder
import org.jsoup.nodes.Document


data class WCResult(val word: String, val occ: Int)

interface Plugin {
    fun scrape(doc: Document): String
}

class WCPlugin: Plugin {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    override fun scrape(doc: Document): String =
        doc.allElements.map { it.text() }
            .flatMap { it.split(" ") }
            .filter { it.find { !it.isLetter() } == null }
            .groupBy { it }
            .mapValues { it.value.size }
            .map { WCResult(it.key, it.value) }
            .let { gson.toJson(it) }
}