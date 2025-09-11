package com.example.danaom

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Query

interface NaverApiService {

    // companion object에 상수들을 정의할 수 있습니다.
    companion object {
        const val BASE_URL = "https://openapi.naver.com/" // 네이버 API 기본 URL
        // 실제 Client ID와 Secret은 안전한 곳에 보관하고, 직접 코드에 하드코딩하지 않는 것이 좋습니다.
        // (예: buildConfigField, local.properties 등)
        const val CLIENT_ID = ""       // 여기에 발급받은 Client ID 입력
        const val CLIENT_SECRET = "" // 여기에 발급받은 Client Secret 입력
    }

    @GET("v1/search/shop.json")
    suspend fun searchNews(
        @Header("X-Naver-Client-Id") clientId: String = CLIENT_ID,
        @Header("X-Naver-Client-Secret") clientSecret: String = CLIENT_SECRET,
        @Query("query") query: String,      // 검색어
        @Query("display") display: Int? = null, // 한 번에 표시할 검색 결과 개수 (기본값: 10, 최댓값: 100)
        @Query("start") start: Int? = null,   // 검색 시작 위치 (기본값: 1, 최댓값: 1000)
        @Query("sort") sort: String? = null    // 정렬 옵션 (sim: 유사도순, date: 날짜순)
    ): Response<NewsResponse> // API 응답 전체를 받기 위해 Response 객체로 감쌈
}