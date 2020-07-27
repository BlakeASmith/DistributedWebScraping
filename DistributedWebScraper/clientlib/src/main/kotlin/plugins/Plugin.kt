import org.jsoup.nodes.Document

interface RoverPlugin {
    fun scrape(doc: Document): String
}