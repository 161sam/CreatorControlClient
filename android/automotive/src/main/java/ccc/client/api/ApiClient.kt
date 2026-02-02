package ccc.client.api

import ccc.client.AppConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val TAG = "CCC"

    @Volatile
    private var baseUrl: String = AppConfig.baseUrl

    private val tokenProvider = InMemoryTokenProvider()

    private val logging: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor { message -> logDebug(message) }
            .apply { level = HttpLoggingInterceptor.Level.BASIC }
    }

    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(BearerTokenInterceptor(tokenProvider))
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private fun createRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Volatile
    var api: CccApi = createRetrofit().create(CccApi::class.java)
        private set

    fun setToken(token: String?) {
        tokenProvider.setToken(token)
    }

    fun downloadExport(downloadUrl: String): Response {
        val resolvedUrl = resolveUrl(downloadUrl)
        val request = Request.Builder()
            .url(resolvedUrl)
            .get()
            .build()
        return okHttp.newCall(request).execute()
    }

    internal fun setBaseUrlForTests(url: String) {
        baseUrl = url
        api = createRetrofit().create(CccApi::class.java)
    }

    private fun resolveUrl(path: String): String {
        return if (path.startsWith("http://") || path.startsWith("https://")) {
            path
        } else {
            baseUrl.trimEnd('/') + path
        }
    }

    private fun logDebug(message: String) {
        runCatching {
            val logClass = Class.forName("android.util.Log")
            val method = logClass.getMethod("d", String::class.java, String::class.java)
            method.invoke(null, TAG, message)
        }
    }
}
