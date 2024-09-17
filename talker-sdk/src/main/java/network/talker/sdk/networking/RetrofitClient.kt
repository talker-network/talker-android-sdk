package network.talker.sdk.networking

import network.talker.sdk.networking.apiRoutes.ApiRoutes
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

internal object RetrofitClient {
    private val loggingInterceptor = HttpLoggingInterceptor()
        .setLevel(HttpLoggingInterceptor.Level.BODY)

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor) // same for .addInterceptor(...)
        .connectTimeout(90, TimeUnit.SECONDS) //Backend is really slow
        .writeTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit
        .Builder()
        .baseUrl(
            ApiRoutes.BASE_URL
        )
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val retrofitApiService = retrofit.create(RetrofitApiService::class.java)
}