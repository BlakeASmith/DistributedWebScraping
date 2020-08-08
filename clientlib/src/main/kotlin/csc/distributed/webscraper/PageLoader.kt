package csc.distributed.webscraper

import org.jsoup.nodes.Document

interface PageLoader: Loader<Document> {
    override fun load(key: String): Document
}