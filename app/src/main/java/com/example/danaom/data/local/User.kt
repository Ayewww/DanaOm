package com.example.danaom


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "user",
    indices = [
        Index(value = ["email","address"], unique = true) // 이 인덱스 때문에 address 프로퍼티 필요
    ]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val uid: Int = 0, // 기본 키 필드
    val name: String,
    val email: String,
    val phonenum: String? = null,
    @ColumnInfo(name = "address")
    val address: String? = null,
    val regiDate: String? = null


) {
    @Ignore
    val tempData: String = ""
}
