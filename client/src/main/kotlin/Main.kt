import csc.distributed.webscraper.kafka.Kafka
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import org.jsoup.Jsoup

@ExperimentalCoroutinesApi
@FlowPreview
fun main(args: Array<String>){
    PLUGIN_TOPIC.defaultConfig = Kafka(
            "plugin-consumer",
            listOf("127.0.0.1:9092"),
            autocommit = false
    )

    pluginResolver.with("wordcount"){
        Jsoup.connect("https://www.google.com").get()
                .let { scrape(it) }
    }.also { println(it) }
}