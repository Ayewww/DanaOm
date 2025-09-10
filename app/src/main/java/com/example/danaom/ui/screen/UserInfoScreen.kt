package com.example.danaom

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun UserInfoScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel( // <--- 수정된 코드
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application
        )
    ), // 또는 hiltViewModel()
    uid: String? // MyPageScreen에서 전달받은 uid
) {

    val userForInfoState by authViewModel.userForInfoScreen.collectAsState()
    val user = userForInfoState

    // 수정할 필드들의 상태 (사용자 정보가 로드되면 초기화)
    var name by remember(user) { mutableStateOf(user?.name ?: "") }
    var email by remember(user) { mutableStateOf(user?.email ?: "") }
    var address by remember(user) { mutableStateOf(user?.address ?: "") }
    var phonenum by remember(user) { mutableStateOf(user?.phonenum ?: "") }
    // regiDate는 보통 수정하지 않으므로 제외하거나 표시만 할 수 있습니다.

    val isLoading by authViewModel.isLoading.collectAsState() // 로딩 상태
    val errorMessage by authViewModel.errorMessage.collectAsState() // 에러 메시지
    val coroutineScope = rememberCoroutineScope()

    // uid가 변경되거나 화면이 처음 로드될 때 사용자 정보 요청
    LaunchedEffect(key1 = uid) {
        if (uid != null && uid != "null") { // "null" 문자열도 체크 (Nav args가 String일 경우)
            authViewModel.loadUserForInfoScreen(uid)
        } else {
            // MyPage에서 "회원 정보 수정"으로 진입한 경우 (uid가 null 또는 "null"일 때)
            // 현재 로그인한 사용자 정보를 로드하도록 ViewModel에 요청
            authViewModel.prepareForMyInfoEdit()
        }
    }

    // 화면에서 벗어날 때 선택된 사용자 정보 초기화 (선택적)
    DisposableEffect(Unit) {
        onDispose {
            // authViewModel.clearUserForInfoScreen()
        }
    }

    // 사용자 정보 수정 성공 시 처리
    LaunchedEffect(authViewModel.userUpdateSuccess) {
        authViewModel.userUpdateSuccess.collect { success ->
            if (success) {
                navController.popBackStack()
                authViewModel.resetUserUpdateSuccess() // 상태 초기화
            }
        }
    }


    if (isLoading && user == null) { // 사용자 정보 로딩 중
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (user == null && uid != null && uid != "null") { // 사용자를 찾을 수 없거나 uid가 유효하지 않은 경우 (초기 로딩 후)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(errorMessage ?: "사용자 정보를 불러올 수 없습니다.")
        }
    } else if (user != null) { // 사용자 정보가 로드된 경우
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {


            Text("회원 정보 수정", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            // UID는 보통 수정 불가, 표시만 하거나 숨김
            Text("UID: ${user.uid}", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("비밀번호") }
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("이메일") }
            )
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("주소") }
            )
            OutlinedTextField(
                value = phonenum,
                onValueChange = { phonenum = it },
                label = { Text("전화번호 (선택)") }
            )
            // regiDate는 보통 수정하지 않음

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    user.copy( // 기존 userForInfoState에서 가져온 user 객체를 복사
                        name = name,
                        email = email,
                        address = address.ifBlank { null },
                        phonenum = phonenum.ifBlank { null }
                    ).let { updatedUser ->
                        authViewModel.updateUserInformation(updatedUser)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading // 로딩 중이 아닐 때만 활성화
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("수정하기")
                }
            }
            Button(
                onClick = {
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4DD0E1), // 예시: 진한 청록색 배경 ( Teal 700 정도)
                    contentColor = Color.White,          // 예시: 흰색 텍스트/아이콘

                    // --- 비활성화 상태일 때의 색상 (선택 사항) ---
                    disabledContainerColor = Color.Gray, // 예시: 회색 배경
                    disabledContentColor = Color.LightGray // 예시: 밝은 회색 텍스트/아이콘
                )
            ) {

                Text("돌아가기")

            }



            errorMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    } else {
        // uid가 null인 경우 등의 초기 에러 상태
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(errorMessage ?: "잘못된 접근입니다.")
        }
    }
}