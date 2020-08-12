package csc.distributed.webscraper.definitions.types

import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Serializable
data class Job(val Id: Int, val Urls: List<String>, val Plugins: List<String>, val Service: String)

@Serializable
data class ScrapingResult(
        val url: String,
        val plugin: String,
        val data: String)

@Serializable
data class JobResult(val jobMetadata: JobMetadata, val results: List<ScrapingResult>)

@Serializable
data class Service(
        val name: String,
        val rootDomains: List<String>,
        val filters: List<String>,
        val plugins: List<String>
)

@Serializable
data class JobMetadata(
        val job: Job,
        val processingTime: Long,
        val numberOfRecordsProduced: Int,
        val numberOfUnreachableSites: Int
){
    val processingTimePerPage = processingTime / job.Urls.size

    class Tracker(val job: Job) {
        private lateinit var startTime: LocalDateTime
        private var numRecords: Int = 0
        private var numSitesUnreachable: Int = 0
        fun begin() = this.apply {
            startTime = LocalDateTime.now()
        }

        fun produceRecord() = this.apply {
            numRecords++
        }

        fun couldNotReachSite() = this.apply {
           numSitesUnreachable++
        }

        fun done() = this.run {
            val endTime = LocalDateTime.now()
            val duration = ChronoUnit.MILLIS.between(startTime, endTime)
            JobMetadata(job, duration, numRecords, numSitesUnreachable)
        }
    }
}






