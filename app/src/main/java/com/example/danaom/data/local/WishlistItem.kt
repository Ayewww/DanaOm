package com.example.danaom

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "wishlist_items",
    primaryKeys = ["userId", "newsItemId"], // 복합 기본 키
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["uid"], // User 테이블의 uid
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE // 사용자가 삭제되면 찜 목록도 삭제
        )
        // NewsItem을 별도 테이블로 관리한다면 ForeignKey 추가 가능
        // 여기서는 NewsItem의 식별자를 직접 저장한다고 가정
    ],
    indices = [Index(value = ["userId"])] // 사용자 ID로 검색을 빠르게 하기 위한 인덱스
)
data class WishlistItem(
    val userId: Int, // User의 uid 참조
    val newsItemId: String, // NewsItem의 고유 식별자 (예: link 또는 상품 ID)
    val title: String?, // 찜 목록에서 바로 보여줄 정보들
    val image: String?,
    val lprice: String?,
    val link: String?, // 원본 링크도 저장
    val addedDate: Long = System.currentTimeMillis() // 찜한 날짜 (선택 사항)
)

