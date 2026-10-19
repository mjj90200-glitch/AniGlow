package com.aniglow.app.data.remote

import com.aniglow.app.BuildConfig
import com.aniglow.app.data.auth.AuthSession
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                AuthSession.accessToken?.takeIf(String::isNotBlank)?.let { token ->
                    requestBuilder.header("Authorization", "Bearer $token")
                }
                chain.proceed(requestBuilder.build())
            }
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val communityApi: CommunityApi by lazy {
        retrofit.create(CommunityApi::class.java)
    }

    val discoveryApi: DiscoveryApi by lazy {
        retrofit.create(DiscoveryApi::class.java)
    }

    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }
}
