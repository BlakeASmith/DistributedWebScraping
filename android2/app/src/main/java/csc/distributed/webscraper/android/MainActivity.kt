package csc.distributed.webscraper.android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.system.Os
import csc.distributed.webscraper.clients.jobProcessorFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.launchIn

class MainActivity : AppCompatActivity() {

    @FlowPreview
    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Os.setenv("WEBSCRAPER_BOOTSTRAPS", "127.0.0.1:9092", true)

        jobProcessorFlow.launchIn(GlobalScope)
    }
}
