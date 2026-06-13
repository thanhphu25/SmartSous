package com.example.smartsous.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.smartsous.data.local.dao.*
import com.example.smartsous.data.local.entity.*
// room db
@Database(
    entities = [
        RecipeEntity::class,
        IngredientEntity::class,
        MealPlanEntity::class,
        ChatMessageEntity::class,
        ChatConversationEntity::class,
        AppNotificationEntity::class,
    ],
    version = 3,
    exportSchema = false
)
abstract class SmartSousDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun appNotificationDao(): AppNotificationDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS chat_conversations (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO chat_conversations (id, title, createdAt, updatedAt)
                    VALUES ('default', 'Đoạn chat cũ', 0, 0)
                    """.trimIndent()
                )
                db.execSQL(
                    "ALTER TABLE chat_messages ADD COLUMN conversationId TEXT NOT NULL DEFAULT 'default'"
                )
                db.execSQL(
                    "ALTER TABLE chat_messages ADD COLUMN type TEXT NOT NULL DEFAULT 'TEXT'"
                )
                db.execSQL(
                    "ALTER TABLE chat_messages ADD COLUMN suggestedRecipesJson TEXT NOT NULL DEFAULT '[]'"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS app_notifications (
                        id TEXT NOT NULL PRIMARY KEY,
                        type TEXT NOT NULL,
                        title TEXT NOT NULL,
                        body TEXT NOT NULL,
                        route TEXT NOT NULL,
                        referenceId TEXT,
                        createdAt INTEGER NOT NULL,
                        readAt INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_app_notifications_createdAt ON app_notifications(createdAt)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_app_notifications_readAt ON app_notifications(readAt)"
                )
            }
        }
    }
}
