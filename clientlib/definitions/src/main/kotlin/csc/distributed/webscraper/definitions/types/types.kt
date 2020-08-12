package csc.distributed.webscraper.definitions.types

import kotlinx.serialization.Serializable

@Serializable
data class Job(val Id: Int, val Urls: List<String>, val Plugins: List<String>, val Service: String)

@Serializable
data class ScrapingResult(
        val url: String,
        val plugin: String,
        val data: String)

@Serializable
data class JobResult(val job: Job, val results: List<ScrapingResult>)

@Serializable
data class Service(
        val name: String,
        val rootDomains: List<String>,
        val filters: List<String>,
        val plugins: List<String>
)





