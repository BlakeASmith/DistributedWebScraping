package csc.distributed.webscraper

import ca.blakeasmith.kkafka.jvm.serialization.JsonSerde
import ca.blakeasmith.kkafka.jvm.serialization.KeyValueSerde
import csc.distributed.webscraper.definitions.types.Job
import csc.distributed.webscraper.definitions.types.JobMetadata
import csc.distributed.webscraper.definitions.types.ScrapingResult
import kotlinx.serialization.json.JsonConfiguration
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes

object ScraperSerdes {
    class Job: JsonSerde<csc.distributed.webscraper.definitions.types.Job>(csc.distributed.webscraper.definitions.types.Job.serializer(), JsonConfiguration.Stable)
    class ScrapingResult: JsonSerde<csc.distributed.webscraper.definitions.types.ScrapingResult>(
            csc.distributed.webscraper.definitions.types.ScrapingResult.serializer(), JsonConfiguration.Stable)
    class Service: JsonSerde<csc.distributed.webscraper.definitions.types.Service>(
            csc.distributed.webscraper.definitions.types.Service.serializer(), JsonConfiguration.Stable)
    class JobMetadata: JsonSerde<csc.distributed.webscraper.definitions.types.JobMetadata>(
            csc.distributed.webscraper.definitions.types.JobMetadata.serializer(), JsonConfiguration.Stable
    )
}

object Serialization {
    class Plugin: KeyValueSerde<String, ByteArray>(Serdes.String(), Serdes.ByteArray())
    class Output: KeyValueSerde<String, ScrapingResult>(Serdes.String(), ScraperSerdes.ScrapingResult())
    class JobMetadata: KeyValueSerde<String?, csc.distributed.webscraper.definitions.types.JobMetadata>(Serdes.String(), ScraperSerdes.JobMetadata())
    class Job: KeyValueSerde<String?, csc.distributed.webscraper.definitions.types.Job>(Serdes.String(), ScraperSerdes.Job())
}
