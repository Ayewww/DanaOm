package com.example.danaom

data class NewsResponse(
    val lastBuildDate: String?,
    val total: Int?,
    val start: Int?,
    val display: Int?,
    val items: List<NewsItem>?
)