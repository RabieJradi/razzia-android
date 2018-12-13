package jradi.rabie.dk.razzia_android.model.api

import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * @author rabie
 *
 *
 */

object HTTPService {

    /**
     * Note that the JsonDeserializers must be defined before calling createService as they otherwise will be null
     */
    val service = createService()


    private val ipAddress = "192.168.0.41"

    private fun createService(): HTTPServiceInterface {

        val gson = GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                .create()

        val clientBuilder = OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .cache(null)

        val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl("http://$ipAddress:8080/")
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(clientBuilder.build())
                .build()

        return retrofit.create(HTTPServiceInterface::class.java)
    }
}