import csc.distributed.webscraper.clients.jobProcessorFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

@ExperimentalCoroutinesApi
@FlowPreview
suspend fun main(args: Array<String>) = jobProcessorFlow.collect()
