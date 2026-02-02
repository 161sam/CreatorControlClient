package ccc.client

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import ccc.client.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private val tag = "CCC"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tv = TextView(this).apply {
            text = "CCC: starting…"
            textSize = 18f
            setPadding(32, 32, 32, 32)
        }
        setContentView(tv)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val res = ApiClient.api.healthz()
                Log.i(tag, "healthz OK: $res")
                withContext(Dispatchers.Main) {
                    tv.text = "CCC: healthz OK\n$res"
                }
            } catch (e: Exception) {
                Log.e(tag, "healthz failed", e)
                withContext(Dispatchers.Main) {
                    tv.text = "CCC: healthz FAILED\n${e.javaClass.simpleName}: ${e.message}"
                }
            }
        }
    }
}
