package com.example.distributedwebscraping
import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.example.distributedwebscraping.grpc.AndroidGrpcClient
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import scrape

val INTERNET = Permission(Manifest.permission.INTERNET)

class MainActivity : PermissionHandler() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun setViewText(text: String){
        val txtview: TextView = findViewById(R.id.textview1)
        txtview.text = text
    }

    fun startWorking(startWorkingButton: View) {
        startWorkingButton as Button
        /*startWorkingButton.text =
            if (startWorkingButton.text == getString(R.string.working_button_text_positive))
                getString(R.string.working_button_text_negative)
            else
                getString(R.string.working_button_text_positive)
         */

        withPermission(INTERNET){
            Log.d("RESP", "requesting work")
            AndroidGrpcClient().requestWork(App.JobRequest.newBuilder().setId(10).build())
                .also {
                    startWorkingButton.text = it.id.toString()
                    val txtview: TextView = findViewById(R.id.textview1)
                    txtview.text = it.urlsList.toString()
                    GlobalScope.launch {
                    }
                }
        }
    }
}