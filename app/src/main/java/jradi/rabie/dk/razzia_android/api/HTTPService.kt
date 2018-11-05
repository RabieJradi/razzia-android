package jradi.rabie.dk.razzia_android.api

import com.google.gson.GsonBuilder
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

    private fun createService(): HTTPServiceInterface {

        val gson = GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                .create()

        val clientBuilder = OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .cache(null)

        val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl("http://localhost:8080/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(clientBuilder.build())
                .build()

        return retrofit.create(HTTPServiceInterface::class.java)
    }
}