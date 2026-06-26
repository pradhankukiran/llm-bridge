package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        LlmConfiguration::class,
        ChatSession::class,
        ChatMessage::class,
        ApiLog::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun llmDao(): LlmDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE llm_configurations ADD COLUMN maxTokens INTEGER NOT NULL DEFAULT 1024")
                db.execSQL("ALTER TABLE llm_configurations ADD COLUMN temperature REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE llm_configurations ADD COLUMN topP REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE llm_configurations ADD COLUMN stream INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE llm_configurations ADD COLUMN extraBodyJson TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE llm_configurations ADD COLUMN extraHeadersJson TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE llm_configurations ADD COLUMN timeoutSeconds INTEGER NOT NULL DEFAULT 180")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE llm_configurations ADD COLUMN providerId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE llm_configurations ADD COLUMN modelOfferingId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE llm_configurations ADD COLUMN reasoningMode TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE llm_configurations ADD COLUMN toolsEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE llm_configurations ADD COLUMN toolDefinitionsJson TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE llm_configurations ADD COLUMN toolChoice TEXT NOT NULL DEFAULT 'auto'")
                db.execSQL("ALTER TABLE llm_configurations ADD COLUMN mediaInputUris TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE llm_configurations ADD COLUMN mediaInputType TEXT NOT NULL DEFAULT 'auto'")
                db.execSQL("ALTER TABLE llm_configurations ADD COLUMN accessTier TEXT NOT NULL DEFAULT 'UNKNOWN'")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE llm_configurations ADD COLUMN systemPrompt TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No schema change between v5 and v6; migration exists only to
                // complete the registered upgrade path so Room can step through
                // every consecutive version without falling back to destructive.
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN mediaUri TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN mediaDisplayName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN mediaInputType TEXT NOT NULL DEFAULT 'auto'")
                db.execSQL("ALTER TABLE api_logs ADD COLUMN sessionId INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_sessionId ON chat_messages(sessionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_api_logs_sessionId ON api_logs(sessionId)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "llm_bridge_database"
                )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7
                )
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
