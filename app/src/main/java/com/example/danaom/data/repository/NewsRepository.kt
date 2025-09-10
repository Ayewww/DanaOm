package com.example.danaom

class NewsRepository(private val apiService: NaverApiService = RetrofitInstance.api) {


    suspend fun searchNews(query: String, display: Int? = 10, start: Int? = 1, sort: String? = "sim"): Result<NewsResponse> {
        return try {
            val response = apiService.searchNews(query = query, display = display, start = start, sort = sort)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Response body is null"))
            } else {
                Result.failure(Exception("API Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
