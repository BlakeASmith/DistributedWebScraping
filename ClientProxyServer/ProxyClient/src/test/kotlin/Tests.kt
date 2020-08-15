import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.http.fullPath
import org.junit.Test

class Tests{

    val client = HttpClient(MockEngine){
        install(JsonFeature){
            serializer = GsonSerializer()
        }

        engine {
            }
    }

    @Test
    fun TestJobReceived(){
    }
}
