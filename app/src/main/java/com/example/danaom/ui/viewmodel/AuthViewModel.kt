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


// LoginState enum (만약 다른 파일에 있다면 해당 파일에서 import 필요)
enum class LoginState {
    IDLE, LOADING, SUCCESS, ERROR_USER_NOT_FOUND, ERROR_INVALID_PASSWORD, ERROR_UNKNOWN
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository: UserRepository // 생성자에서 주입받거나 init에서 초기화 필요

    // --- User & Admin State ---
    private val _isCurrentUserAdmin = MutableStateFlow(false)
    val isCurrentUserAdmin: StateFlow<Boolean> = _isCurrentUserAdmin.asStateFlow()

    private val _userForInfoScreen = MutableStateFlow<User?>(null)
    val userForInfoScreen: StateFlow<User?> = _userForInfoScreen.asStateFlow()

    private val ADMIN_EMAIL = "admin"

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _userUpdateSuccess = MutableSharedFlow<Boolean>()
    val userUpdateSuccess: SharedFlow<Boolean> = _userUpdateSuccess.asSharedFlow()

    // --- Common State ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // --- Login State ---
    private val _currentLoggedInUserUid = MutableStateFlow<String?>(null)
    val currentLoggedInUserUid: StateFlow<String?> = _currentLoggedInUserUid.asStateFlow()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.IDLE)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _loggedInUser = MutableStateFlow<User?>(null)
    val loggedInUser: StateFlow<User?> = _loggedInUser.asStateFlow()

    // --- Wishlist State ---
    private val _wishlistStatusMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val wishlistStatusMap: StateFlow<Map<String, Boolean>> = _wishlistStatusMap.asStateFlow()

    private val _wishlist = MutableStateFlow<List<WishlistItem>>(emptyList())
    val wishlist: StateFlow<List<WishlistItem>> = _wishlist.asStateFlow()

    // --- Admin User Management State ---
    private val _allUsersList = MutableStateFlow<List<User>>(emptyList())
    val allUsersList: StateFlow<List<User>> = _allUsersList.asStateFlow()

    private val _selectedUserForAdmin = MutableStateFlow<User?>(null)
    val selectedUserForAdmin: StateFlow<User?> = _selectedUserForAdmin.asStateFlow()

    private val _adminScreenIsLoading = MutableStateFlow(false)
    val adminScreenIsLoading: StateFlow<Boolean> = _adminScreenIsLoading.asStateFlow()

    private val _adminScreenErrorMessage = MutableStateFlow<String?>(null)
    val adminScreenErrorMessage: StateFlow<String?> = _adminScreenErrorMessage.asStateFlow()

    // --- Product Detail State ---
    private val _selectedProduct = MutableStateFlow<NewsItem?>(null)
    val selectedProduct: StateFlow<NewsItem?> = _selectedProduct.asStateFlow()

    private val _productLoadingError = MutableStateFlow<String?>(null)
    val productLoadingError: StateFlow<String?> = _productLoadingError.asStateFlow()

    init {
        val userDao = AppDatabase.getDatabase(application).userDao()
        userRepository = UserRepository(userDao) // UserRepository 인스턴스화

        viewModelScope.launch {
            _currentLoggedInUserUid.collect { uidString ->
                if (uidString != null) {
                    uidString.toIntOrNull()?.let { userId ->
                        // 전체 찜 목록(_wishlist) 로드
                        userRepository.getWishlistItems(userId).collect { itemsFromDb ->
                            _wishlist.value = itemsFromDb
                            // _wishlistStatusMap도 함께 업데이트 (newsItemId가 상품 link라고 가정)
                            _wishlistStatusMap.value = itemsFromDb.associate { it.newsItemId to true }
                        }
                    }
                } else {
                    _wishlist.value = emptyList()
                    _wishlistStatusMap.value = emptyMap()
                }
            }
        }

        viewModelScope.launch {
            _currentUser.collect { user ->
                val isAdmin = (user?.email == ADMIN_EMAIL)
                _isCurrentUserAdmin.value = isAdmin
                if (isAdmin) {
                    loadAllUsersForAdmin() // 관리자면 사용자 목록 로드
                } else {
                    _allUsersList.value = emptyList() // 관리자 아니면 비움
                }
            }
        }
    }

    fun fetchWishlistStatusForNewsItems(newsItems: List<NewsItem>) {
        val userId = _currentLoggedInUserUid.value?.toIntOrNull() ?: run {
            _wishlistStatusMap.value = newsItems.mapNotNull { it.link }.associateWith { false }
            return
        }
        val newsItemLinks = newsItems.mapNotNull { it.link }

        if (newsItemLinks.isEmpty()) {
            return
        }

        viewModelScope.launch {
            try {
                // UserRepository에 getWishlistStatusForItems 함수가 구현되어 있다고 가정
                val statusMapFromDb = userRepository.getWishlistStatusForItems(userId, newsItemLinks)
                _wishlistStatusMap.value = statusMapFromDb
            } catch (e: Exception) {
                _errorMessage.value = "찜 목록 상태 로드 실패: ${e.message}"
                Log.e("AuthViewModel", "Error fetching wishlist status for items", e)
                _wishlistStatusMap.value = newsItemLinks.associateWith { false }
            }
        }
    }

    fun toggleWishlistItem(newsItem: NewsItem) {
        val userId = _currentLoggedInUserUid.value?.toIntOrNull() ?: return
        val itemLink = newsItem.link ?: return

        viewModelScope.launch {
            val isCurrentlyWishlisted = _wishlistStatusMap.value[itemLink] ?: false
            try {
                if (isCurrentlyWishlisted) {
                    userRepository.removeWishlistItemByUserAndNewsItem(userId, itemLink)
                    _wishlistStatusMap.update { currentMap ->
                        currentMap.toMutableMap().apply { remove(itemLink) }
                    }
                } else {
                    val wishlistItem = WishlistItem(
                        userId = userId,
                        newsItemId = itemLink,
                        title = newsItem.title,
                        image = newsItem.image,
                        lprice = newsItem.lprice,
                        link = newsItem.originallink,
                        addedDate = System.currentTimeMillis()
                    )
                    userRepository.addWishlistItem(wishlistItem)
                    _wishlistStatusMap.update { currentMap ->
                        currentMap.toMutableMap().apply { this[itemLink] = true }
                    }
                }
                // 선택적: _wishlist (List<WishlistItem>)도 DB 변경 후 갱신
                // loadWishlistForCurrentUser() // 이런 함수를 만들어 호출 가능
            } catch (e: Exception) {
                _errorMessage.value = "찜하기 처리 중 오류: ${e.message}"
                Log.e("AuthViewModel", "Error toggling wishlist item", e)
            }
        }
    }

    fun loadProductDetailByEncodedLink(
        encodedLink: String?,
        currentNewsListFromNewsViewModel: List<NewsItem>
    ) {
        _productLoadingError.value = null
        _selectedProduct.value = null

        if (encodedLink == null) {
            _productLoadingError.value = "제품 ID가 없습니다."
            return
        }

        viewModelScope.launch {
            try {
                val decodedLink = URLDecoder.decode(encodedLink, StandardCharsets.UTF_8.toString())
                val foundItem = currentNewsListFromNewsViewModel.find { it.link == decodedLink }

                if (foundItem != null) {
                    _selectedProduct.value = foundItem
                } else {
                    _selectedProduct.value = null
                    _productLoadingError.value = "현재 표시된 목록에 해당 제품이 없습니다. (링크: $decodedLink)"
                    Log.w("AuthViewModel", "Product not found in current list: $decodedLink. List size: ${currentNewsListFromNewsViewModel.size}")
                }
            } catch (e: Exception) {
                _selectedProduct.value = null
                _productLoadingError.value = "제품 정보 로드 중 오류 발생: ${e.message}"
                Log.e("AuthViewModel", "Error in loadProductDetailByEncodedLink", e)
            }
        }
    }

    fun clearSelectedProduct() {
        _selectedProduct.value = null
        _productLoadingError.value = null
    }

    fun isProductWishlisted(productLink: String?): Boolean {
        return productLink?.let { _wishlistStatusMap.value[it] } ?: false
    }

    fun toggleWishlistForProduct(product: NewsItem?) {
        product?.let {
            toggleWishlistItem(it)
        }
    }

    fun registerUser(user: User) {
        viewModelScope.launch {
            try {
                userRepository.registerUser(user)
                // 성공 처리
            } catch (e: Exception) {
                _errorMessage.value = "회원가입 실패: ${e.message}"
            }
        }
    }

    fun login(email: String, enteredPasswordAttempt: String) {
        _loginState.value = LoginState.LOADING
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val user = userRepository.loginUser(email)
                if (user != null) {
                    if (user.name == enteredPasswordAttempt) { // 비밀번호 필드를 user.name으로 가정
                        setLoggedInUser(user)
                        _loginState.value = LoginState.SUCCESS
                    } else {
                        _loginState.value = LoginState.ERROR_INVALID_PASSWORD
                        _errorMessage.value = "비밀번호가 일치하지 않습니다."
                    }
                } else {
                    _loginState.value = LoginState.ERROR_USER_NOT_FOUND
                    _errorMessage.value = "해당 이메일로 가입된 사용자가 없습니다."
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.ERROR_UNKNOWN
                _errorMessage.value = "로그인 중 오류 발생: ${e.message}"
            }
        }
    }

    fun setLoggedInUser(user: User) {
        _loggedInUser.value = user
        _currentUser.value = user
        _currentLoggedInUserUid.value = user.uid.toString()
        // 관리자 여부 확인은 init의 _currentUser.collect에서 처리
        Log.d("AuthViewModel", "Logged in user UID set: ${user.uid}")
    }

    fun logout() {
        _loggedInUser.value = null
        _currentUser.value = null
        _currentLoggedInUserUid.value = null
        // _isCurrentUserAdmin.value는 _currentUser.collect에 의해 자동으로 false가 됨
        _loginState.value = LoginState.IDLE
        _wishlistStatusMap.value = emptyMap() // 찜 목록 상태도 초기화
        _wishlist.value = emptyList() // 찜 목록 리스트도 초기화
        Log.d("AuthViewModel", "User logged out.")
    }

    fun loadUserById(uid: String) { // UserInfoScreen 등에서 특정 사용자 정보 로드 시
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val userIdAsInt = uid.toIntOrNull()
            if (userIdAsInt == null) {
                _errorMessage.value = "유효하지 않은 사용자 ID 형식입니다."
                _userForInfoScreen.value = null
                _isLoading.value = false
                return@launch
            }
            try {
                val userFromDb = userRepository.getUserById(userIdAsInt)
                _userForInfoScreen.value = userFromDb
                if (userFromDb == null) {
                    _errorMessage.value = "사용자 정보를 찾을 수 없습니다."
                }
            } catch (e: Exception) {
                _errorMessage.value = "사용자 정보 로드 실패: ${e.message}"
                _userForInfoScreen.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateUserInformation(updatedUser: User) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                userRepository.updateUser(updatedUser)
                _userForInfoScreen.value = updatedUser // 현재 화면 정보 업데이트

                if (_currentUser.value?.uid == updatedUser.uid) { // 현재 로그인 사용자 정보도 업데이트
                    _currentUser.value = updatedUser
                    _loggedInUser.value = updatedUser
                }
                _userUpdateSuccess.emit(true)
            } catch (e: Exception) {
                _errorMessage.value = "정보 수정 실패: ${e.message}"
                _userUpdateSuccess.emit(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetUserUpdateSuccess() {
        // SharedFlow의 경우 별도 리셋 불필요
    }

    fun loadAllUsersForAdmin() {
        if (!_isCurrentUserAdmin.value) return

        viewModelScope.launch {
            _adminScreenIsLoading.value = true
            _adminScreenErrorMessage.value = null
            try {
                userRepository.allUsers.collect { users -> // allUsers가 Flow<List<User>>라고 가정
                    _allUsersList.value = users
                }
            } catch (e: Exception) {
                _adminScreenErrorMessage.value = "사용자 목록 로드 실패: ${e.message}"
            } finally {
                _adminScreenIsLoading.value = false
            }
        }
    }

    fun selectUserForAdmin(user: User?) {
        _selectedUserForAdmin.value = user
    }

    fun adminAddOrUpdateUser(user: User, isNewUser: Boolean) {
        if (!_isCurrentUserAdmin.value) return

        viewModelScope.launch {
            _adminScreenIsLoading.value = true
            _adminScreenErrorMessage.value = null
            try {
                userRepository.adminAddUser(user) // UserRepository에 해당 함수 필요
                _adminScreenErrorMessage.value = if (isNewUser) "사용자 추가 성공" else "사용자 정보 수정 성공"
                selectUserForAdmin(null)
            } catch (e: Exception) {
                _adminScreenErrorMessage.value = "작업 실패: ${e.message}"
            } finally {
                _adminScreenIsLoading.value = false
            }
        }
    }

    fun adminDeleteSelectedUser() {
        if (!_isCurrentUserAdmin.value) return

        _selectedUserForAdmin.value?.let { userToDelete ->
            viewModelScope.launch {
                _adminScreenIsLoading.value = true
                _adminScreenErrorMessage.value = null
                try {
                    userToDelete.uid?.let { userRepository.adminDeleteUserById(it) } // UserRepository에 해당 함수 필요
                    _adminScreenErrorMessage.value = "사용자 '${userToDelete.email}' 삭제 성공"
                    selectUserForAdmin(null)
                } catch (e: Exception) {
                    _adminScreenErrorMessage.value = "삭제 실패: ${e.message}"
                } finally {
                    _adminScreenIsLoading.value = false
                }
            }
        } ?: run {
            _adminScreenErrorMessage.value = "삭제할 사용자를 선택해주세요."
        }
    }

    // UserInfoScreen 진입 시, 현재 로그인한 사용자 정보를 로드하기 위한 함수


    fun loadUserForInfoScreen(uid: String) { // UserInfoScreen 등에서 특정 사용자 정보 로드 시
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null // 이전 에러 메시지 초기화
            val userIdAsInt = uid.toIntOrNull()
            if (userIdAsInt == null) {
                _errorMessage.value = "유효하지 않은 사용자 ID 형식입니다."
                _userForInfoScreen.value = null
                _isLoading.value = false
                return@launch
            }
            try {
                val userFromDb = userRepository.getUserById(userIdAsInt) // UserRepository에 해당 함수 필요
                _userForInfoScreen.value = userFromDb
                if (userFromDb == null) {
                    _errorMessage.value = "사용자 ID ${userIdAsInt}에 해당하는 정보를 찾을 수 없습니다."
                }
            } catch (e: Exception) {
                _errorMessage.value = "사용자 정보 로드 실패: ${e.message}"
                _userForInfoScreen.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun prepareForMyInfoEdit() {
        _userForInfoScreen.value = _currentUser.value // 현재 로그인한 사용자 정보로 설정
        _errorMessage.value = null // 이전 에러 메시지 초기화
    }
}

