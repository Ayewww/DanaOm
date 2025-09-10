package com.example.danaom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    navController: NavController,
    authViewModel: AuthViewModel // 또는 AdminViewModel
) {
    val allUsers by authViewModel.allUsersList.collectAsState()
    val selectedUser by authViewModel.selectedUserForAdmin.collectAsState()
    val isLoading by authViewModel.adminScreenIsLoading.collectAsState()
    val errorMessage by authViewModel.adminScreenErrorMessage.collectAsState() // ViewModel에서 메시지 관리

    // 화면 진입 시 사용자 목록 로드 (ViewModel init에서 이미 하고 있다면 생략 가능)
    LaunchedEffect(Unit) {
        authViewModel.loadAllUsersForAdmin()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("사용자 관리") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate("register")
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "사용자 추가")
                    }
                }
            )
        },
        snackbarHost = { // 간단한 메시지 표시에 SnackBar 사용
            errorMessage?.let {
                SnackbarHost(hostState = remember { SnackbarHostState() }) { data ->
                    Snackbar(snackbarData = data)
                }
                // 메시지 표시 후 ViewModel의 메시지 상태 초기화 (선택적)
                // LaunchedEffect(errorMessage) { authViewModel.clearAdminScreenErrorMessage() }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (isLoading && allUsers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (allUsers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("등록된 사용자가 없습니다.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allUsers, key = { it.uid ?: it.hashCode() }) { user ->
                        UserListItem(
                            user = user,
                            onEditClick = {
                                    userIdString ->
                                navController.navigate("userInfo/$userIdString")
                            },
                            onDeleteClick = {
                                authViewModel.selectUserForAdmin(user) // 삭제할 사용자 선택
                                authViewModel.adminDeleteSelectedUser() // ViewModel에 삭제 요청
                            }
                        )
                    }
                }
            }

            if (isLoading && allUsers.isNotEmpty()) { // 목록 하단 로딩 (페이징 시)
                Box(
                    modifier = Modifier
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

@Composable
fun UserListItem(
    user: User,
    onEditClick: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("UID: ${user.uid ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                Text(user.email ?: "이메일 없음", style = MaterialTheme.typography.titleMedium)
                Text(user.name ?: "이름 없음 (비밀번호)", style = MaterialTheme.typography.bodyMedium)
                Text("주소: ${user.address ?: "-"}", style = MaterialTheme.typography.bodySmall)
                Text("전화번호: ${user.phonenum ?: "-"}", style = MaterialTheme.typography.bodySmall)
                Text("가입일: ${user.regiDate ?: "-"}", style = MaterialTheme.typography.bodySmall)
            }
            Row {
                IconButton(onClick = {
                    user.uid?.toString()?.let { userIdString -> // uid가 Int라면 String으로 변환
                        onEditClick(userIdString) // uid 전달
                    }
                }) {
                    Icon(Icons.Filled.Edit, contentDescription = "수정")
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Filled.Delete, contentDescription = "삭제", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}