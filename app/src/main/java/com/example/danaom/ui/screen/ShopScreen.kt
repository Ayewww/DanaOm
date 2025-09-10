package com.example.danaom
// NewsScreen.kt (또는 MainActivity.kt 내부에)

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExposedDropdownMenuDefaults.outlinedTextFieldColors
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel // viewModel() 헬퍼
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(modifier: Modifier = Modifier,
               newsViewModel: NewsViewModel = viewModel(), // NewsViewModel이 일반 ViewModel이고, 인자 없는 기본 생성자 또는 모든 인자에 기본값이 있다면 OK
               authViewModel: AuthViewModel, // AuthViewModel은 기존 방식 유지 (AndroidViewModel + factory)
               navController: NavController) {

    val newsItems: List<NewsItem> by newsViewModel.newsList.collectAsState()
    val loadState by newsViewModel.loadState.collectAsState()
    val errorMessage by newsViewModel.errorMessage
    var searchQuery by remember { mutableStateOf("") } // 기본 검색어 또는 빈 문자열
    val currentSortOption by newsViewModel.currentSortOption
    val loggedInUserUid by authViewModel.currentLoggedInUserUid.collectAsState()
    val wishlistStatusMap by authViewModel.wishlistStatusMap.collectAsState()

    val listState = rememberLazyListState()

    // newsItems가 변경될 때마다 해당 아이템들의 찜 상태를 가져오도록 요청
    LaunchedEffect(newsItems, loggedInUserUid) { // loggedInUserUid도 키로 추가
        if (loggedInUserUid != null && newsItems.isNotEmpty()) {
            authViewModel.fetchWishlistStatusForNewsItems(newsItems)

        } else if (loggedInUserUid == null) {
            // 로그아웃 상태면 찜 상태맵을 비우도록 ViewModel에 요청 (ViewModel init에서 이미 처리 중일 수 있음)
            // authViewModel.clearWishlistStatus() // 이런 함수를 ViewModel에 만들 수 있음
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo ->
                val totalItems = layoutInfo.totalItemsCount
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                if (lastVisible >= totalItems - 3 && loadState == LoadState.IDLE) {
                    newsViewModel.loadNextItems()
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .height(70.dp)
                .fillMaxWidth()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("상품 검색") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp), // 높이를 약간 늘려줌
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7CFC00),
                    unfocusedBorderColor = Color(0xFF7CFC00)
                ),
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 60.sp // 글씨 위아래 공간 확보
                ),
                trailingIcon = {
                    IconButton(
                        onClick = { newsViewModel.searchNews(searchQuery) },
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color(0xFF0000), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "검색 실행",
                            tint = Color.Cyan
                        )
                    }
                }
            )

        }



        Spacer(modifier = Modifier.height(16.dp))
        Spacer(modifier = Modifier.width(50.dp))
        Text("정렬", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp) // 버튼 사이 간격
        ) {
            SortOption.entries.forEach { sortOption -> // 모든 SortOption enum 값 순회
                Button(
                    onClick = { newsViewModel.changeSortOption(sortOption, searchQuery) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentSortOption == sortOption) Color.LightGray else Color(0xFF0000),
                        contentColor = Color.Black // 텍스트 색상, 필요에 따라 변경
                    ),
                    modifier = Modifier
                        .size(width = 70.dp, height = 46.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(sortOption.displayName)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))


        // 로딩 및 에러 상태 표시
        if (loadState == LoadState.LOADING && newsItems.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
        } else if (newsItems.isEmpty() && loadState == LoadState.IDLE && errorMessage == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("해당 상품이 존재하지 않습니다.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState // 👈 무한 스크롤용 상태 추가
            ) {
                items(
                    items = newsItems,
                    key = { newsItem -> newsItem.link ?: newsItem.hashCode() }
                ) { newsItem ->// 각 newsItem에 대해 반복

                    // --- isWishlisted 변수를 여기서 정의 ---
                    val isWishlisted = wishlistStatusMap[newsItem.link] ?: false
                    // newsItem.link를 ID로 사용한다고 가정, newsItem.link가 null일 수 있으므로 안전 호출 또는 다른 ID 사용
                    // 또는 newsItem에 고유 ID가 있다면 그것을 사용:
                    // val newsItemId = newsItem.productId ?: newsItem.link // 예시: productId가 있다면 우선 사용
                    // val isWishlisted = if (newsItemId != null) wishlistStatusMap[newsItemId] ?: false else false

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                newsItem.link?.let { link ->
                                    val encodedUrl =
                                        URLEncoder.encode(link, StandardCharsets.UTF_8.toString())
                                    navController.navigate("webview/$encodedUrl")
                                }
                            },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF0F0F0) // 원하는 배경색으로 변경
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            newsItem.image?.let { imageUrl ->
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = newsItem.title?.removeHtmlTags() ?: "상품 이미지",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = newsItem.title?.removeHtmlTags() ?: "이름 없음",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (loggedInUserUid != null) {
                                    Checkbox(
                                        checked = isWishlisted, // <--- 여기서 isWishlisted 사용
                                        onCheckedChange = {
                                            authViewModel.toggleWishlistItem(newsItem)
                                        }
                                    )
                                }
                            }
                            // Spacer(Modifier.height(8.dp)) // 이 Spacer는 아마도 Row 안이 아니라 Column 바로 아래에 있어야 할 것 같습니다.
                            // 가격 정보를 표시하는 Row와 위의 제목/체크박스 Row는 별개의 Row여야 합니다.
                            // 아래의 가격 정보 Row는 위의 Row 밖, Column 내부에 있어야 합니다.
                            // --- 가격 정보 Row 시작 ---
                            Spacer(Modifier.height(8.dp)) // 제목/체크박스 Row와 가격 Row 사이의 간격
                            Row { // 가격 정보를 위한 새로운 Row
                                Text(
                                    text = "최저가 :",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "₩",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = newsItem.lprice?.removeHtmlTags() ?: "재고 없음",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = newsItem.mallName?.removeHtmlTags() ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                            }
                            // --- 가격 정보 Row 끝 ---
                        }

                    }
                }
                if (loadState == LoadState.LOADING && newsItems.isNotEmpty()) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}


@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NewsWebView(url: String) { // 파라미터 이름 url로 통일
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                webViewClient = WebViewClient() // 링크 클릭 시 앱 내 WebView에서 열리도록
                loadUrl(url) // 디코딩은 WebView가 내부적으로 처리하거나, 필요시 명시적 디코딩
            }
        },
        modifier = Modifier.fillMaxSize() // WebView가 전체 화면을 채우도록
    )
}


