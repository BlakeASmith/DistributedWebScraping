package csc.distributed.webscraper.plugins
import org.jsoup.nodes.Document

interface Plugin {
    fun scrape(doc: Document): String
}