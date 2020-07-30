import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.Reader
import kotlin.reflect.KClass

fun Any.json(gson: Gson = GsonBuilder().setPrettyPrinting().create()): String = gson.toJson(this)
fun <T : Any> KClass<T>.fromJson(json: String, gson: Gson = Gson()) =gson.fromJson(json, java)
fun <T : Any> KClass<T>.fromJson(json: Reader, gson: Gson = Gson()) =gson.fromJson(json, java)
