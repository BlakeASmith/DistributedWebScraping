package csc.distributed.webscraper.utils

import kotlinx.coroutines.Deferred

suspend fun <T, R> withDeferred(deffered: Deferred<T>, op: T.() -> R) = deffered.await().run(op)
