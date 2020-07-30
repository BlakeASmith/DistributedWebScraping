import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

class ArgumentParser<T, R>(private val convert: (String) -> T) {
    private val required = mutableSetOf<String>()
    private val concurrent by lazy {
        mutableMapOf<String, suspend (T) -> R>()
    }

    fun async(key: String, required: Boolean = false, op: suspend T.() -> R) = this.apply {
        concurrent[key] = op
        if (required)
            require(key)
    }

    fun require(key: String) = this.apply {
        required.add(key)
    }

    fun parseAsync(args: Array<String>, scope: CoroutineScope = GlobalScope) =
        args.also {
            required.forEach {
                assert(it in args)
            }
        }.withIndex().associateBy { it }
                .filter { it.key.value in concurrent }
                .mapValues { (_, v) ->
                    scope.async {
                        concurrent[v.value]!!(convert(args[v.index+1]))
                    }
                }.mapKeys { it.key.value }

    fun parse(args: Array<String>, scope: CoroutineScope = GlobalScope) = runBlocking {
        parseAsync(args, scope).mapValues { it.value.await() }
    }
}