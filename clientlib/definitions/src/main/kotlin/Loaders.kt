package csc.distributed.webscraper.types

import org.jsoup.nodes.Document

interface Loader<T>{
    fun load(key: String): T
}

fun <T> loader(load: (String) -> T) = object : Loader<T> {
    override fun load(key: String): T = load(key)
}

object JsoupLoader: Loader<Document> {
    override fun load(url: String): Document = org.jsoup.Jsoup.connect(url).get()
}
