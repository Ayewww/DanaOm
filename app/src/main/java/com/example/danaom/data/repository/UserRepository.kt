package com.example.danaom

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext


class UserRepository(private val userDao: UserDao) {

    suspend fun registerUser(user: User) {
        Log.d("UserRepository", "Attempting to register user: ${user.email}")
        try {
            userDao.insertUser(user)
            Log.d("UserRepository", "User registered successfully: ${user.email}")
        } catch (e: Exception) {
            Log.e("UserRepository", "Error registering user: ${user.email}", e)
        }
    }

    // 로그인 시 사용: 이메일로 사용자 정보를 가져옴
    suspend fun loginUser(email: String): User? {
        return userDao.getUserByEmail(email)
    }

    // 모든 사용자 목록 가져오기 (Flow 사용 예시) - 프로퍼티만 남깁니다.
    val allUsers: Flow<List<User>> = userDao.getAllUsers()

    suspend fun getLoggedInUser(): User? { return userDao.getLastUser() }

    suspend fun getUserById(uid: Int): User? {
        return withContext(Dispatchers.IO) {
            userDao.getUserById(uid)
        }
    }

    suspend fun updateUser(user: User) {
        withContext(Dispatchers.IO) {
            userDao.updateUser(user)
        }
    }

    suspend fun addWishlistItem(item: WishlistItem) {
        userDao.insertWishlistItem(item)
    }

    suspend fun removeWishlistItem(item: WishlistItem) {
        userDao.deleteWishlistItem(item)
    }

    suspend fun removeWishlistItemByUserAndNewsItem(userId: Int, newsItemId: String) {
        userDao.deleteWishlistItemByUserAndNewsItem(userId, newsItemId)
    }

    fun getWishlistItems(userId: Int): Flow<List<WishlistItem>> {
        return userDao.getWishlistItemsByUserId(userId)
    }

    suspend fun isNewsItemInWishlist(userId: Int, newsItemId: String): Boolean {
        return userDao.getWishlistItemByUserAndNewsItem(userId, newsItemId) != null
    }

    suspend fun getWishlistStatusForItems(userId: Int, newsItemIds: List<String>): Map<String, Boolean> {
        val wishlistedItems = userDao.getWishlistItemsByUserAndNewsItemIds(userId, newsItemIds)
        return newsItemIds.associateWith { id -> wishlistedItems.any { it.newsItemId == id } }
    }

    // fun getAllUsers(): Flow<List<User>> { // <--- 이 함수를 삭제합니다.
    //     return userDao.getAllUsers()
    // }

    suspend fun adminAddUser(user: User) {
        userDao.insertOrUpdateUser(user)
    }

    suspend fun adminUpdateUser(user: User) {
        userDao.updateUser(user)
    }

    suspend fun adminDeleteUser(user: User) {
        userDao.deleteUser(user)
    }

    suspend fun adminDeleteUserById(userId: Int) {
        userDao.deleteUserById(userId)
    }
}