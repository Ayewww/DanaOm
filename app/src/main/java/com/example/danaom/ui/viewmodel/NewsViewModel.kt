package com.example.danaom
// NewsViewModel.kt
import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.State
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.lifecycle.ViewModelProvider
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

enum class SortOption(val value: String, val displayName: String) {
    SIM("sim", "정확도순"), // API 지원
    DATE("date", "날짜순"), // API 지원
    PRICE_ASC("asc", "  가격\n낮은 순"), // 클라이언트 측 정렬
    PRICE_DESC("desc", "  가격\n높은 순")  // 클라이언트 측 정렬
}

enum class LoadState {
    IDLE,
    LOADING,
    ERROR,
    REACHED_END
}

class NewsViewModel(
    private val repository: NewsRepository = NewsRepository() // 두 번째 인자: NewsRepository (기본값 있음)
) : ViewModel() { // Repository 주입 고려

    private val _newsListFlow = MutableStateFlow<List<NewsItem>>(emptyList())
    val newsList: StateFlow<List<NewsItem>> = _newsListFlow.asStateFlow()


    // --- 무한 스크롤 및 상세 로딩 상태 ---
    private var currentPage = 1 // Naver API의 'start'는 1부터 시작
    private var currentQueryInternal = "" // 내부에서 사용할 검색어
    private var currentApiSortInternal = SortOption.SIM.value // 내부에서 사용할 API 정렬 파라미터

    private val _loadState = MutableStateFlow(LoadState.IDLE)
    val loadState: StateFlow<LoadState> = _loadState.asStateFlow()

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private val _currentSortOptionUi = mutableStateOf(SortOption.SIM)
    val currentSortOption: State<SortOption> = _currentSortOptionUi

    private fun getApiSortParamForRequest(sortOption: SortOption): String {
        return when (sortOption) {
            SortOption.SIM -> SortOption.SIM.value
            SortOption.DATE -> SortOption.DATE.value
            SortOption.PRICE_ASC -> SortOption.PRICE_ASC.value
            SortOption.PRICE_DESC -> SortOption.PRICE_DESC.value
        }
    }

    // 클라이언트 측에서 정렬하는 함수
    private fun sortNewsListClientSide(list: List<NewsItem>, sortOption: SortOption): List<NewsItem> {
        return when (sortOption) {
            SortOption.PRICE_ASC -> list.sortedBy { it.lprice?.filter { char -> char.isDigit() }?.toIntOrNull() ?: Int.MAX_VALUE }
            SortOption.PRICE_DESC -> list.sortedByDescending { it.lprice?.filter { char -> char.isDigit() }?.toIntOrNull() ?: Int.MIN_VALUE }
            else -> list // SIM, DATE는 API에서 이미 정렬되도록 요청했거나, 여기서 추가 처리 불필요
        }
    }

    fun searchNews(query: String) {
        // ... (기존 로직 대부분 유지 가능)
        // 만약 이 안에서 getApplication<>().getString(...) 처럼 컨텍스트를 썼다면 수정 필요
        // 하지만 현재 코드에서는 그런 부분이 없어 보입니다.
        if (query.isBlank()) {
            _errorMessage.value = "검색어를 입력해주세요."
            _newsListFlow.value = emptyList()
            _loadState.value = LoadState.IDLE // 추가: 검색어 없을 때 로드 상태 초기화
            return
        }
        currentQueryInternal = query
        currentApiSortInternal = getApiSortParamForRequest(_currentSortOptionUi.value)
        currentPage = 1
        _newsListFlow.value = emptyList()
        _errorMessage.value = null
        _loadState.value = LoadState.IDLE // 추가: 새 검색 시 로드 상태 초기화
        fetchNewsItems(isInitialLoad = true)
    }

    // 정렬 옵션 변경 함수
    fun changeSortOption(newSortOption: SortOption, currentQueryFromUi: String) {
        if (_currentSortOptionUi.value == newSortOption && _loadState.value != LoadState.LOADING) {
            return
        }
        _currentSortOptionUi.value = newSortOption
        currentApiSortInternal = getApiSortParamForRequest(newSortOption)
        currentQueryInternal = currentQueryFromUi
        currentPage = 1
        _newsListFlow.value = emptyList()
        _errorMessage.value = null
        _loadState.value = LoadState.IDLE
        if (currentQueryInternal.isNotBlank()) {
            fetchNewsItems(isInitialLoad = true)
        } else {
            _newsListFlow.value = emptyList()
            _loadState.value = LoadState.IDLE
        }
    }

    // 다음 페이지 아이템 로드 함수
    fun loadNextItems() {
        if (_loadState.value == LoadState.LOADING || _loadState.value == LoadState.REACHED_END) {
            return
        }
        fetchNewsItems(isInitialLoad = false)
    }

    private fun fetchNewsItems(isInitialLoad: Boolean) {
        if (currentQueryInternal.isBlank() && isInitialLoad) {
            _loadState.value = LoadState.IDLE
            _newsListFlow.value = emptyList()
            return
        }
        if (currentQueryInternal.isBlank() && !isInitialLoad) {
            _loadState.value = LoadState.REACHED_END
            return
        }

        _loadState.value = LoadState.LOADING
        viewModelScope.launch {
            repository.searchNews(
                query = currentQueryInternal,
                display = 20,
                start = currentPage,
                sort = currentApiSortInternal
            ).fold(
                onSuccess = { response ->
                    _errorMessage.value = null
                    val fetchedItems = response.items ?: emptyList()
                    val processedItems = fetchedItems // 정렬은 API에서 처리 가정

                    if (processedItems.isNotEmpty()) {
                        if (isInitialLoad) {
                            _newsListFlow.value = processedItems
                        } else {
                            _newsListFlow.value = _newsListFlow.value + processedItems
                        }
                        currentPage += response.display ?: 20

                        val totalLoadedCount = _newsListFlow.value.size
                        if (response.total != null && totalLoadedCount >= response.total) {
                            _loadState.value = LoadState.REACHED_END
                        } else {
                            _loadState.value = LoadState.IDLE
                        }
                    } else {
                        if (isInitialLoad) {
                            _newsListFlow.value = emptyList()
                            _loadState.value = LoadState.IDLE
                        } else {
                            _loadState.value = LoadState.REACHED_END
                        }
                    }
                },
                onFailure = { exception ->
                    _errorMessage.value = "데이터 로드 실패: ${exception.message}"
                    _loadState.value = LoadState.ERROR
                }
            )
        }
    }
}