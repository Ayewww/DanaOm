package com.example.danaom


import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow // 비동기 데이터 스트림을 위해 Flow 사용 (선택 사항)

@Dao
interface UserDao {

    // 사용자 정보 삽입 (이미 존재하는 사용자는 무시하거나 교체할 수 있음)
    @Insert(onConflict = OnConflictStrategy.IGNORE) // 또는 .REPLACE
    suspend fun insertUser(user: User)

    // 이메일로 사용자 정보 조회 (로그인 시 사용)
    @Query("SELECT * FROM user WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User? // suspend: 코루틴에서 비동기 실행

    // UID로 사용자 정보 조회 (필요한 경우)
    @Query("SELECT * FROM user WHERE uid = :uid LIMIT 1")
    suspend fun getUserByUid(uid: Int): User?

    // 모든 사용자 정보 조회 (Flow를 사용하여 UI에 실시간 업데이트 가능 - 선택 사항)
    @Query("SELECT * FROM user ORDER BY name ASC")
    fun getAllUsers(): Flow<List<User>>

    @Update
    suspend fun updateUser(user: User) // 사용자 정보 업데이트 함수
    @Query("SELECT * FROM user ORDER BY uid DESC LIMIT 1") // 예: uid가 가장 큰 사용자
    suspend fun getLastUser(): User? // getLastUser 함수 정의 및 SQL 쿼리

    @Query("SELECT * FROM user WHERE uid = :uid LIMIT 1")
    suspend fun getUserById(uid: Int): User? // ID로 사용자 조회 함수

    @Insert(onConflict = OnConflictStrategy.REPLACE) // 이미 있으면 덮어쓰거나, IGNORE로 할 수도 있음
    suspend fun insertWishlistItem(item: WishlistItem)

    @Delete
    suspend fun deleteWishlistItem(item: WishlistItem)

    // 특정 사용자의 모든 찜 목록 가져오기
    @Query("SELECT * FROM wishlist_items WHERE userId = :userId ORDER BY addedDate DESC")
    fun getWishlistItemsByUserId(userId: Int): Flow<List<WishlistItem>>

    // 특정 사용자가 특정 상품을 찜했는지 확인 (단일 아이템)
    @Query("SELECT * FROM wishlist_items WHERE userId = :userId AND newsItemId = :newsItemId LIMIT 1")
    suspend fun getWishlistItemByUserAndNewsItem(userId: Int, newsItemId: String): WishlistItem?

    // 여러 newsItemId에 대해 찜 여부 확인 (효율성을 위해)
    @Query("SELECT * FROM wishlist_items WHERE userId = :userId AND newsItemId IN (:newsItemIds)")
    suspend fun getWishlistItemsByUserAndNewsItemIds(userId: Int, newsItemIds: List<String>): List<WishlistItem>

    @Query("DELETE FROM wishlist_items WHERE userId = :userId AND newsItemId = :newsItemId")
    suspend fun deleteWishlistItemByUserAndNewsItem(userId: Int, newsItemId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE) // 관리자가 추가/수정 시
    suspend fun insertOrUpdateUser(user: User) // 추가 및 수정에 사용 가능

    @Delete
    suspend fun deleteUser(user: User)
    @Query("DELETE FROM user WHERE uid = :userId")
    suspend fun deleteUserById(userId: Int)

}