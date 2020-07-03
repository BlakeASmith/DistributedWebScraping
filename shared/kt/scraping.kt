import kotlinx.coroutines.runBlocking

fun scrape(urls: List<String>): Sequence<String> = sequence {
    urls.forEach {
        runBlocking {
            kotlinx.coroutines.delay(100)
        }
        yield(it)
    }
}