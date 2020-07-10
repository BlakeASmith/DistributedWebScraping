package main.plugin

import org.jsoup.nodes.Document

interface Plugin<T> {
    // this class must reflect the schema of a cassandra table
    // it must also be serializable using Gson
    val dbTableClass: Class<T>
    fun scrape(document: Document): T
}