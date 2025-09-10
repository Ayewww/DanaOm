package com.example.danaom

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: (User) -> Unit, // 로그인 성공 시 호출될 콜백
    onNavigateToRegister: () -> Unit // 회원가입 화면으로 이동할 콜백
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") } // 실제로는 ViewModel에서 관리하는 것이 더 좋을 수 있음

    val loginState by authViewModel.loginState.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()
    val loggedInUser by authViewModel.loggedInUser.collectAsState()

    // 로그인 성공 시 콜백 호출
    LaunchedEffect(loggedInUser) {
        if (loginState == LoginState.SUCCESS) {
            // authViewModel.loggedInUser.value는 이때 이미 설정되어 있어야 함
            authViewModel.loggedInUser.value?.let { user ->
                onLoginSuccess(user)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("로그인", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("이메일") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("비밀번호") }, // User 엔티티의 name을 비밀번호로 사용한다고 가정 (실제로는 금지!)
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { authViewModel.login(email, password) },
            enabled = loginState != LoginState.LOADING,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                // --- 활성화 상태일 때의 색상 ---
                containerColor = Color(0xFF4DD0E1), // 예시: 진한 청록색 배경 ( Teal 700 정도)
                contentColor = Color.White,          // 예시: 흰색 텍스트/아이콘

                // --- 비활성화 상태일 때의 색상 (선택 사항) ---
                disabledContainerColor = Color.Gray, // 예시: 회색 배경
                disabledContentColor = Color.LightGray // 예시: 밝은 회색 텍스트/아이콘
            )
        ) {
            if (loginState == LoginState.LOADING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("로그인")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onNavigateToRegister) {
            Text("회원가입")
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        if (loginState == LoginState.ERROR_INVALID_PASSWORD) {
            // 구체적인 에러 메시지 처리
        }
    }
}