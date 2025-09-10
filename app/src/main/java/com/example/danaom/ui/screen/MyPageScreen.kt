package com.example.danaom

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

@Composable
fun MyPageScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel( // <--- 수정된 코드
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val currentUidState by authViewModel.currentLoggedInUserUid.collectAsState()
    val currentUid = currentUidState
    val isUserAdmin by authViewModel.isCurrentUserAdmin.collectAsState() // 관리자 상태 관찰


    if (currentUid == null) {
        // ... (로그인 정보 없을 때 UI)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("로그인 정보가 없습니다. 로그인 페이지로 이동합니다.")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }) {
                    Text("로그인 하러 가기")
                }
            }
        }
    } else {
        // Column으로 전체 화면을 구성하고, 로그아웃 버튼을 맨 아래로 밀어냄
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), // 전체 화면에 대한 패딩
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "마이 페이지",
                style = MaterialTheme.typography.headlineMedium, // 스타일 약간 변경
                modifier = Modifier.padding(bottom = 32.dp) // 하단 패딩 늘림
            )

            // --- 메뉴 리스트 시작 ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp) // 좌우 패딩 추가하여 중앙 정렬된 느낌
            ) {
                // 메뉴 아이템 1: 찜 목록
                MyPageMenuItem(
                    text = "찜 목록",
                    onClick = {
                        currentUid.let { uid ->
                            navController.navigate("wishlist/$uid")
                        }
                    }
                )

                // 메뉴 아이템 사이 구분선 (선택적)
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // 메뉴 아이템 2: 회원 정보 수정
                MyPageMenuItem(
                    text = "회원 정보 수정",
                    onClick = {
                        // currentUid.let { uid -> // uid를 전달할 필요 없이 "null" 또는 빈 문자열 전달
                        navController.navigate("userInfo/null") // "null"은 현재 사용자 정보 수정을 의미
                        // }
                    }
                )

                // 관리자일 경우에만 표시되는 메뉴
                if (isUserAdmin) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    MyPageMenuItem(
                        text = "사용자 관리",
                        onClick = {
                            navController.navigate("admin")
                        }
                    )
                }
            }
            // --- 이 Spacer가 로그아웃 버튼을 맨 아래로 밀어냅니다 ---
            Spacer(modifier = Modifier.weight(1f))
            // ---------------------------------------------------

            Button(
                onClick = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4DD0E1), // 예시: 진한 청록색 배경 ( Teal 700 정도)
                    contentColor = Color.White,          // 예시: 흰색 텍스트/아이콘

                    // --- 비활성화 상태일 때의 색상 (선택 사항) ---
                    disabledContainerColor = Color.Gray, // 예시: 회색 배경
                    disabledContentColor = Color.LightGray // 예시: 밝은 회색 텍스트/아이콘
                )

            ) {
                Text("로그아웃")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun MyPageMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp), // 클릭 영역 및 내부 패딩
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium // 메뉴 텍스트 스타일
        )
        Spacer(modifier = Modifier.weight(1f)) // 텍스트를 왼쪽으로 밀고, 오른쪽에 아이콘 등을 위한 공간 확보 (선택적)
        // Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) // > 아이콘 (선택적)
    }
}


fun String.removeHtmlTags(): String {
    return this.replace(Regex("<.*?>"), "")
}