package com.cariad.m2.sharebar.map

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 高德地图api接口
 */
interface AmapApiService {
    /**
     * https://lbs.amap.com/api/webservice/guide/api-advanced/newpoisearch
     * 搜索poi2.0版本：关键字搜索
     */
    @GET("v5/place/text?parameters")
    suspend fun searchPlaces(
        @Query("key") key: String = "dfc8e44f9ee6849b97d93842071de6ac",
        @Query("keywords") keywords: String = "",
        @Query("page_num") page_num: Int = 1,
        @Query("page_size") page_size: Int = 1,
    ): Response<SearchResponse>
}