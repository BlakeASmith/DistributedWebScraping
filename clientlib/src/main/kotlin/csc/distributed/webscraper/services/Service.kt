package csc.distributed.webscraper.services

import kotlinx.serialization.Serializable

@Serializable
data class Service(
    val name: String,
    val rootDomains: List<String>,
    val filters: List<String>,
    val plugins: List<String>
)


