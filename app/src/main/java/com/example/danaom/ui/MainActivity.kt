package com.example.danaom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import kotlin.getValue
import com.example.danaom.ui.theme.DanaOmTheme


class MainActivity : ComponentActivity() {
    // AuthViewModel을 Activity 범위에서 생성
    private val authViewModel: AuthViewModel by viewModels() // androidx.activity.viewModels 사용
    val startColor = Color(0xFF7CFC00)
    val endColor = Color(0xFF4DD0E1)

    // 그라데이션 브러시 생성
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(startColor, endColor)
        // 필요에 따라 listOf(startColor, midColor1, midColor2, ..., endColor) 와 같이 중간색 추가 가능
    )

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DanaOmTheme {
                val navController = rememberNavController()
                // 로그인 상태를 관찰하여 TopAppBar의 아이콘을 동적으로 변경
                val loggedInUserUidState = authViewModel.currentLoggedInUserUid.collectAsState() // State<String?>
                val isLoggedIn = loggedInUserUidState.value != null // Boolean


                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0000))
                ) {
                    Scaffold(
                        containerColor = Transparent,
                        topBar = {
                            TopAppBar(
                                title = {
                                    TextButton(
                                        onClick = {
                                            navController.navigate("shop") {
                                                launchSingleTop = true // shop이 이미 최상단이면 새 인스턴스 만들지 않음
                                                // 다른 화면에서 shop으로 올 때 이전 스택을 정리하고 싶다면 추가 설정
                                                // popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                            }
                                        },
                                    ) {
                                        Text(
                                            text = "다나옴",
                                            fontSize = 27.sp,
                                            style = TextStyle(
                                                brush = gradientBrush,
                                                fontWeight = FontWeight.Bold// 텍스트 스타일에 브러시 적용
                                                // alpha 값은 graphicsLayer에서 조절하므로 여기서는 설정 안 함
                                            ),
                                            modifier = Modifier.graphicsLayer(alpha = 0.99f) // 약간의 트릭: alpha를 살짝 조절하여 브러시가 적용되도록 함
                                        )
                                    }
                                },
                                actions = {
                                    IconButton(onClick = {
                                        // "mypage" 라우트로 이동 요청, AppNavigation에서 로그인 상태에 따라 분기
                                        navController.navigate("mypage")
                                    }) {
                                        Icon(Icons.Filled.AccountCircle, "마이페이지", tint = Color(0xFF4DD0E1))
                                    }
                                    // 로그인 상태에 따라 아이콘 및 동작 변경
                                    if (isLoggedIn) {
                                        IconButton(onClick = {
                                            authViewModel.logout()
                                            // 로그아웃 후 로그인 화면 또는 초기 화면으로 이동
                                            navController.navigate("shop") { // 또는 "login"
                                                popUpTo(navController.graph.startDestinationId) {
                                                    inclusive = true
                                                }
                                                launchSingleTop = true
                                            }
                                        }) {
                                            Icon(Icons.Filled.ExitToApp, "로그아웃", tint = Color(0xFF4DD0E1)) // 아이콘도 변경 가능
                                        }
                                    } else {
                                        IconButton(onClick = {
                                            navController.navigate("auth_graph") {
                                                launchSingleTop = true
                                            }
                                        }) {
                                            Icon(Icons.Filled.ExitToApp, "로그인", tint = Color(0xFF4DD0E1))
                                        }
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Transparent
                                )
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        AppNavigation(
                            navController = navController,
                            modifier = Modifier.padding(innerPadding),
                            authViewModel = authViewModel // AppNavigation으로 Activity 범위의 AuthViewModel 전달
                        )
                    }
                }
            }
        }
    }
}
