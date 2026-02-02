package ccc.client

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ccc.client.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private val tag = "CCC"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tv = TextView(this).apply {
            text = "Connecting…"
            setTextColor(Color.GRAY)
            textSize = 18f
            setPadding(32, 32, 32, 32)
        }
        setContentView(tv)

        lifecycleScope.launch {
            try {
                Log.i(tag, "healthz starting")
                val res = withContext(Dispatchers.IO) { ApiClient.api.healthz() }
                Log.i(tag, "healthz OK: $res")
                tv.text = "OK: $res"
                tv.setTextColor(Color.rgb(46, 125, 50))
            } catch (e: Exception) {
                Log.e(tag, "healthz failed", e)
                tv.text = "ERROR: ${e.javaClass.simpleName}: ${e.message}"
                tv.setTextColor(Color.rgb(198, 40, 40))
            }
        }
    }
}
