package com.example.danaom

import android.app.Application
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel = viewModel( // <--- 수정된 코드
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application
        )
    ),
    onRegisterSuccess: () -> Unit, // 회원가입 성공 시 호출될 콜백
    onNavigateToLogin: () -> Unit // 로그인 화면으로 이동할 콜백
) {
    var uid by remember { mutableStateOf("") } // 실제로는 자동 생성 또는 다른 방식
    var name by remember { mutableStateOf("") } // 비밀번호로 사용될 예정 (실제로는 금지!)
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") } // User 엔티티의 인덱스 때문에 필요
    var phonenum by remember { mutableStateOf("") }
    var regiDate by remember { mutableStateOf("") } // 예: "YYYY-MM-DD"

    val errorMessage by authViewModel.errorMessage.collectAsState() // ViewModel의 에러 메시지 관찰

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("회원가입", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("이메일") })
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("비밀번호") })
        OutlinedTextField(
            value = phonenum,
            onValueChange = { phonenum = it },
            label = { Text("전화번호 (선택)") })
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("주소") }) // 인덱스 때문에 필요
        OutlinedTextField(
            value = regiDate,
            onValueChange = { regiDate = it },
            label = { Text("가입일 (선택, YYYY-MM-DD)") })

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                // UID 유효성 검사 등 필요
                val userToRegister = User(
                    name = name, // 비밀번호로 사용 (실제로는 금지!)
                    email = email,
                    phonenum = phonenum.ifBlank { null },
                    address = address.ifBlank { null }, // address가 비어있으면 null로 (인덱스 때문에 필요)
                    regiDate = regiDate.ifBlank { null }
                )
                Log.d("RegisterScreen", "User object being sent to ViewModel: $userToRegister")
                authViewModel.registerUser(userToRegister)
                onRegisterSuccess() // 임시로 바로 성공 처리
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("가입하기")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onNavigateToLogin) {
            Text("이미 계정이 있으신가요? 로그인")
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}