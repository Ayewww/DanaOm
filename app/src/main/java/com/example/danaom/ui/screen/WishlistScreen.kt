package com.example.danaom

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    navController: NavController,
    authViewModel: AuthViewModel, // MainActivity에서 전달받은 AuthViewModel
    uid: String? // MyPageScreen에서 전달받은 사용자 UID
) {
    // ViewModel에서 현재 로그인된 사용자의 UID를 가져오거나, 파라미터로 받은 uid 사용
    // 여기서는 파라미터로 받은 uid를 우선적으로 사용한다고 가정
    val currentUserId = uid?.toIntOrNull()

    // ViewModel에서 전체 찜 목록 상태를 가져옴
    // 이 Flow는 AuthViewModel의 init 블록에서 이미 해당 사용자의 찜 목록을 구독하고 있어야 함
    val wishlistItems by authViewModel.wishlist.collectAsState()
    // 로딩 상태 (필요하다면 ViewModel에 추가)
    // val isLoading by authViewModel.isLoadingWishlist.collectAsState() // 예시
    // 에러 메시지 (필요하다면 ViewModel에 추가)
    // val errorMessage by authViewModel.wishlistErrorMessage.collectAsState() // 예시

    val context = LocalContext.current // Toast 메시지용

    // 화면이 처음 로드될 때 또는 사용자 ID가 변경될 때 찜 목록을 명시적으로 로드해야 한다면,
    // ViewModel에 해당 함수를 만들고 여기서 호출할 수 있습니다.
    // AuthViewModel의 init에서 currentLoggedInUserUid를 collect하여 자동으로 로드한다면 이 부분은 필요 없을 수 있습니다.
    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            // authViewModel.loadWishlistByUserId(currentUserId) // 예: ViewModel에 이런 함수가 있다면 호출
            // 이미 AuthViewModel의 init에서 currentLoggedInUserUid를 기준으로 wishlist를 로드하고 있다면,
            // 이 LaunchedEffect는 uid가 변경될 때 추가적인 작업을 하기 위해 남겨둘 수 있습니다.
            // 예를 들어, uid가 null이면 이전 화면으로 돌려보내는 등의 로직.
            if (uid == null) {
                Toast.makeText(context, "사용자 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
        } else {
            Toast.makeText(context, "잘못된 사용자 접근입니다.", Toast.LENGTH_SHORT).show()
            navController.popBackStack() // 이전 화면으로
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("찜 목록") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }, ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (wishlistItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("찜한 상품이 없습니다.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(wishlistItems, key = { it.userId.toString() + "_" + it.newsItemId }) { wishlistItem ->
                        WishlistItemCard(
                            wishlistItem = wishlistItem,
                            onItemClick = {
                                wishlistItem.link?.let { link ->
                                    val encodedUrl = URLEncoder.encode(link, StandardCharsets.UTF_8.toString())
                                    navController.navigate("webview/$encodedUrl")
                                }
                            },
                            onRemoveClick = {
                                // 찜 해제 기능: AuthViewModel의 toggleWishlistItem 또는 별도의 remove 함수 호출
                                // NewsItem 객체가 필요하므로, WishlistItem에서 NewsItem으로 변환하거나
                                // AuthViewModel에 WishlistItem을 직접 받아 처리하는 함수 필요
                                val newsItemEquivalent = NewsItem( // WishlistItem을 NewsItem으로 변환 (ID만 있어도 됨)
                                    title = wishlistItem.title,
                                    link = wishlistItem.link, // 고유 식별자로 link 사용 가정
                                    image = wishlistItem.image,
                                    lprice = wishlistItem.lprice,
                                    // 나머지 필드는 찜 해제 로직에 따라 필요 없을 수 있음
                                    originallink = null, mallName = null
                                )
                                authViewModel.toggleWishlistItem(newsItemEquivalent) // 기존 toggle 함수 재활용
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WishlistItemCard(
    wishlistItem: WishlistItem,
    onItemClick: () -> Unit,
    onRemoveClick: () -> Unit // 찜 해제 콜백
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            wishlistItem.image?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = wishlistItem.title ?: "상품 이미지",
                    modifier = Modifier
                        .size(80.dp) // 작은 이미지 크기
                        .padding(end = 8.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = wishlistItem.title?.removeHtmlTags() ?: "이름 없음",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "최저가: ₩${wishlistItem.lprice?.removeHtmlTags() ?: "정보 없음"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // 찜 해제 버튼 (선택 사항)
            IconButton(onClick = onRemoveClick) {
                Icon(Icons.Filled.Delete, contentDescription = "찜 해제", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}