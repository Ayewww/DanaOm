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

@Database(entities = [User::class, WishlistItem::class], version = 3, exportSchema = false) // 엔티티와 버전 명시
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao // DAO 접근자

    companion object {
        @Volatile // 다른 스레드에서 이 변수에 접근할 때 항상 최신 값을 가지도록 보장
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // WishlistItem 테이블 생성 쿼리
                db.execSQL("CREATE TABLE IF NOT EXISTS `wishlist_items` (`userId` INTEGER NOT NULL, `newsItemId` TEXT NOT NULL, `title` TEXT, `image` TEXT, `lprice` TEXT, `link` TEXT, `addedDate` INTEGER NOT NULL, PRIMARY KEY(`userId`, `newsItemId`), FOREIGN KEY(`userId`) REFERENCES `user`(`uid`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                // WishlistItem 테이블에 필요한 인덱스 생성 쿼리 (선택 사항이지만 성능에 도움)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_wishlist_items_userId` ON `wishlist_items` (`userId`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) { // 여러 스레드에서 동시에 접근하는 것을 방지
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "user_database" // 데이터베이스 파일 이름
                )
                    .addMigrations(MIGRATION_2_3) // 데이터베이스 마이그레이션이 필요한 경우
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
