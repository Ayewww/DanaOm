package com.example.danaom

data class NewsItem(
    val title: String?,
    val originallink: String?,
    val link: String?,
    val image: String?,
    val lprice: String?,
    val mallName: String?,
    val isSelected: Boolean = false
)