package com.cariad.m2.sharebar.map

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * 用来创建高德地图Api服务对象
 */
fun provideAmapRetrofit(): AmapApiService {
    val httpClient = OkHttpClient.Builder()
        .readTimeout(5000, TimeUnit.SECONDS)
        .connectTimeout(5000, TimeUnit.SECONDS)
        .writeTimeout(5000, TimeUnit.SECONDS)
        .build()

    return Retrofit.Builder()
        .baseUrl("https://restapi.amap.com/")
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AmapApiService::class.java)
}