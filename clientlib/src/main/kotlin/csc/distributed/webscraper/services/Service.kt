package csc.distributed.webscraper.services

data class Service(
    val name: String,
    val rootDomains: List<String>,
    val filters: List<String>,
    val destination: Pair<String, Int> = "127.0.0.1" to 9696,
    val plugins: List<String>
)


