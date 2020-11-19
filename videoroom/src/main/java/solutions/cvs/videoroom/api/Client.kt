package solutions.cvs.videoroom.api

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.*
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.await
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URL


interface Refresher {
    fun doRefresh()
}

/**
 * API client
 */
class Client(baseURL: URL) {

    /**
     * Current session data if this client is authenticated, otherwise null
     */
    val sessionData: LiveData<SessionData?>
        get() = liveSessionData


    private var api: API;
    private val liveSessionData = MutableLiveData<SessionData?>()
    private interface API : UserSessionAPI, ConferenceAPI {}

    class CookieJar : okhttp3.CookieJar {
        private var cookies: List<Cookie>? = null

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            this.cookies = cookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookies ?: ArrayList()
        }
    }


    class AuthenticationInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            val originalRequest = chain.request();
            val request = chain.request().newBuilder().build()
            val response = chain.proceed(request)
            return response
        }
    }

    init {
        val builder = OkHttpClient.Builder()
        builder.interceptors().add(AuthenticationInterceptor())
        val okHttpClient = builder.cookieJar(CookieJar()).build()
        api = Retrofit.Builder()
            .baseUrl(baseURL)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .client(okHttpClient)
            .build().create(API::class.java)
    }

    suspend fun login(username: String, password: String): UserData? {
        try {
            val sessionData = api.login(LoginRequest(username, password)).await()
            liveSessionData.value = sessionData
            return sessionData.userData
        } catch (e: HttpException) {
            if (e.code() < 500) return null
            else throw e
        }

    }

    suspend fun logoff() {
        try {
            api.logoff().await()
        } finally {
            liveSessionData.value = null
        }
    }
}