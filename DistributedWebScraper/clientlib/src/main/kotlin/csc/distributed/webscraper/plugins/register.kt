package csc.distributed.webscraper.plugins
import csc.distributed.webscraper.kafka.Kafka
import java.io.File
import java.io.FileInputStream

fun registerPlugin(name: String, jar: File) {
    Kafka("plugin_upload")
        .pushToTopic("plugins", name, FileInputStream(jar).readAllBytes())
}

