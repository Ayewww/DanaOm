package com.example.danaom

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel // MainActivity로부터 전달받은 Activity 범위의 ViewModel
) {
    NavHost(
        navController = navController,
        startDestination = "shop", // 앱 시작 시 첫 화면
        modifier = modifier
    ) {
        composable("shop") {
            // NewsScreen에 Activity 범위의 AuthViewModel 전달
            NewsScreen(
                navController = navController,
                authViewModel = authViewModel
                // newsViewModel은 NewsScreen 내부에서 viewModel()로 생성
            )
        }

        // 인증 관련 화면들을 그룹화하는 중첩 네비게이션
        navigation(
            startDestination = "login", // 이 네비게이션 그래프의 시작점
            route = "auth_graph"        // 이 네비게이션 그래프의 라우트 이름
        ) {
            val authGraphRoute = "auth_graph" // 중복 방지용 상수

            composable("login") {
                // LoginScreen에 Activity 범위의 AuthViewModel 전달
                LoginScreen(
                    authViewModel = authViewModel,
                    onLoginSuccess = { user ->
                        // 로그인 성공 시 이전 화면(shop 또는 다른 화면)으로 돌아가거나,
                        // 마이페이지로 바로 이동할 수 있음.
                        // auth_graph를 스택에서 제거하고 이전 화면으로 돌아가는 예시:
                        navController.popBackStack(route = authGraphRoute, inclusive = true)
                        // 또는 특정 화면으로 이동
                        // navController.navigate("shop") { // 또는 "mypage_in_auth"
                        //    popUpTo(authGraphRoute) { inclusive = true }
                        // }
                        // 현재는 mypage_in_auth로 이동 (MyPageScreen도 authViewModel 공유)
                        // navController.navigate("mypage_in_auth") {
                        // popUpTo("login") { inclusive = true } // login 화면만 제거
                        // }
                    },
                    onNavigateToRegister = {
                        navController.navigate("register") // register 화면으로 이동
                    }
                )
            }
            composable("register") {
                // RegisterScreen에 Activity 범위의 AuthViewModel 전달
                RegisterScreen(
                    authViewModel = authViewModel,
                    onRegisterSuccess = {
                        // 회원가입 성공 시 로그인 화면으로 이동하고 현재 화면(register)은 스택에서 제거
                        navController.navigate("login") {
                            popUpTo("register") { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack() // 이전 화면(login)으로 돌아감
                    }
                )
            }
            composable("mypage_in_auth") { // 로그인된 사용자만 접근하는 마이페이지
                // MyPageScreen에 Activity 범위의 AuthViewModel 전달
                MyPageScreen(
                    navController = navController,
                    authViewModel = authViewModel
                )
            }
            composable(
                route = "userInfo/{uid}", // uid를 파라미터로 받음
                arguments = listOf(navArgument("uid") {
                    type = NavType.StringType
                    nullable = true
                }) // uid 파라미터 정의
            ) { backStackEntry ->
                val uid = backStackEntry.arguments?.getString("uid")
                // UserInfoScreen에 Activity 범위의 AuthViewModel 전달
                UserInfoScreen(
                    navController = navController,
                    authViewModel = authViewModel,
                    uid = uid // 전달받은 uid 사용
                )
            }
            composable(
                route = "wishlist/{uid}", // 기존 라우트 이름 유지
                arguments = listOf(navArgument("uid") { type = NavType.StringType })
            ) { backStackEntry ->
                val uidArgument = backStackEntry.arguments?.getString("uid")
                // WishlistScreen에 Activity 범위의 AuthViewModel과 uid 전달
                WishlistScreen(
                    navController = navController,
                    authViewModel = authViewModel,
                    uid = uidArgument // 전달받은 uid 사용
                )
            }
            composable("admin") { // 로그인된 사용자만 접근하는 마이페이지
                // MyPageScreen에 Activity 범위의 AuthViewModel 전달
                val isAdmin by authViewModel.isCurrentUserAdmin.collectAsState()
                val context = LocalContext.current // Toast 메시지용

                if (isAdmin) {
                    AdminScreen(navController = navController, authViewModel = authViewModel)
                } else {
                    // 관리자가 아닌 경우, 이전 화면으로 돌려보내거나 경고 메시지를 표시합니다.
                    // 이 LaunchedEffect는 화면 구성 시 한 번 실행되어 접근을 제어합니다.
                    LaunchedEffect(Unit) {
                        Toast.makeText(context, "접근 권한이 없습니다.", Toast.LENGTH_SHORT).show()
                        navController.popBackStack() // 이전 화면으로 돌아감
                    }
                }
            }
        }

        // TopAppBar의 "마이페이지" 아이콘 클릭 시 사용될 중간 라우트
        composable("mypage") {
            // 로그인 상태 확인 후 적절한 화면으로 이동
            val loggedInUserUidState = authViewModel.currentLoggedInUserUid.collectAsState() // State<String?>
            val isLoggedInLocal = loggedInUserUidState.value != null // Boolean, 변수명 변경 (옵션)
            // LaunchedEffect는 키가 변경될 때마다 실행됨. 로그인 상태 변경 시 네비게이션 트리거
            LaunchedEffect(isLoggedInLocal, navController) { // 수정된 isLoggedInLocal 사용
                if (isLoggedInLocal) {
                    navController.navigate("mypage_in_auth") {
                        popUpTo("mypage") { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    navController.navigate("login") {
                        popUpTo("mypage") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }

        composable(
            route = "webview/{encodedUrl}",
            arguments = listOf(navArgument("encodedUrl") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedUrlValue = backStackEntry.arguments?.getString("encodedUrl")
            if (encodedUrlValue != null) {
                NewsWebView(url = encodedUrlValue) // NewsWebView는 authViewModel이 필요 없을 수 있음
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("유효한 URL이 없습니다.")
                }
            }
        }

    }
}