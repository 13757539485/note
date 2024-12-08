package com.cariad.m2.sharebar.map

/**
 * 高德地图结果数据类
 */
data class SearchResponse(
    val status: String,
    val info: String,
    val infocode: String,
    val count: String,
    val pois: ArrayList<Pois>,
)

data class Pois(
    val name: String,
    val id: String,
    val location: String,
    val typecode: String,
    val pname: String,
    val cityname: String,
    val adname: String,
    val address: String,
    val pcode: String,
    val adcode: String,
    val citycode: String
)