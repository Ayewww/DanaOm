package com.example.danaom

// RetrofitInstance.kt
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    // OkHttp 로깅 인터셉터 설정 (개발 시 유용)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // 요청/응답 바디까지 로깅
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor) // 로깅 인터셉터 추가
        .build()

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(NaverApiService.BASE_URL)
            .client(httpClient) // 커스텀 OkHttpClient 사용
            .addConverterFactory(GsonConverterFactory.create()) // JSON 변환기로 Gson 사용
            .build()
    }

    val api: NaverApiService by lazy {
        retrofit.create(NaverApiService::class.java)
    }
}


