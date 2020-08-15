package csc.distributed.webscraper.config


const val CONFIG_BOOTSTRAPS = "WEBSCRAPER_BOOTSTRAPS"

data class Config(
        val bootstraps: List<String>
) {
    companion object {
        fun get() = Config(System.getenv(CONFIG_BOOTSTRAPS).split(','))
    }
}