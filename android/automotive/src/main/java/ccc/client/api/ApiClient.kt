package ccc.client.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object ApiClient {

    private const val TAG = "CCC"

    // Für echtes Device (LAN): nimm deine PC-IP
    // Wenn du USB reverse nutzt: "http://127.0.0.1:4828/"
    // WICHTIG: Retrofit braucht trailing slash!
    private const val BASE_URL = "http://127.0.0.1:4828/"

    private val tokenProvider = InMemoryTokenProvider()

    private val logging: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor { message -> android.util.Log.d(TAG, message) }
            .apply { level = HttpLoggingInterceptor.Level.BASIC }
    }

    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(BearerTokenInterceptor(tokenProvider))
            .addInterceptor(logging)
            .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val api: CccApi by lazy {
        retrofit.create(CccApi::class.java)
    }

    fun setToken(token: String?) {
        tokenProvider.setToken(token)
    }
}
