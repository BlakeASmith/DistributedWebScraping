import csc.distributed.webscraper.plugins.Plugin
import org.jsoup.nodes.Document

class TestPlugin: Plugin {
    override fun scrape(doc: Document): String {
        return "{\"test\":\"${doc.location()}\"}"
    }
}