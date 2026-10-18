package com.aurevant.pkk.di

import com.aurevant.pkk.core.network.ApiService
import com.aurevant.pkk.core.network.AuthInterceptor
import com.aurevant.pkk.core.network.SessionStore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Ganti BASE_URL saat backend bukan di emulator: IP LAN mesin + :8000/api/v1/ */
private const val BASE_URL = "http://10.0.2.2:8000/api/v1/"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun gson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun okHttp(auth: AuthInterceptor): OkHttpClient {
        val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        return OkHttpClient.Builder()
            .addInterceptor(auth)
            .addInterceptor(log)
            .build()
    }

    @Provides
    @Singleton
    fun api(gson: Gson, client: OkHttpClient): ApiService =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
}
