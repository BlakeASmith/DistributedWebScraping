package csc.distributed.webscraper.android

import Permission
import PermissionHandler
import android.Manifest
import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import csc.distributed.webscraper.definitions.types.ScrapingResult
import csc.distributed.webscraper.proxy.ProxyClient
import csc.distributed.webscraper.types.JsoupLoader
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.HttpTimeout
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.apache.http.conn.ssl.TrustAllStrategy
import org.apache.http.impl.client.HttpClients
import org.apache.http.ssl.SSLContextBuilder
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

val INTERNET = Permission(Manifest.permission.INTERNET)

class MainActivity : PermissionHandler() {

    lateinit var displayFlow: Flow<ScrapingResult>

    @FlowPreview
    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mainContent = findViewById<TextView>(R.id.main_content)

        runBlocking {
            HttpClient(Apache){
                install(HttpTimeout){
                    socketTimeoutMillis = Long.MAX_VALUE
                }
            }.run {
                get<String>("http://192.168.1.147:8080/")
                .also { mainContent.text = it }
                .also { println(it) }

                get<String>("http://192.168.1.147:8080/jobs")
                .also { mainContent.text = it }
                .also { println(it) }
            }
        }

        mainContent.text = "${INTERNET.check(this)}"

        val client = ProxyClient(HttpClient(Apache){
            install(JsonFeature){
                serializer = GsonSerializer()
            }

        }, "http://192.168.1.147:8080", JsoupLoader, true)


        client.errors.consumeAsFlow()
                .onEach { runOnUiThread { mainContent.text = it.message } }
                .launchIn(GlobalScope)

        runBlocking {
            client.start(transform = {
                onEach { runOnUiThread { mainContent.text = it.toString() } }
            }){
                mainContent.text = "thread launched"
                onEach { println(it) }
            }
        }

    }

    //collectServerInfoFromUser()

private fun collectServerInfoFromUser() {
    val view = LayoutInflater.from(applicationContext)
            .inflate(R.layout.server_info_input_layout, null)

        val mainContent = findViewById<TextView>(R.id.main_content)

        AlertDialog.Builder(this)
            .setTitle("Server Information")
            .setView(view)
            .setPositiveButton("Submit"){ dialog, _ ->
                dialog as AlertDialog
                val host = dialog.findViewById<EditText>(R.id.input_host)
                val port = dialog.findViewById<EditText>(R.id.input_port)

            }
            .show()
    }

}
