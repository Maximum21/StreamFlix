package com.asadraza.streamflix.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2){
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXITS remote_keys (
            id TEXT NOT NULL PRIMARY KEY,
            category_or_query TEXT NOT NULL,
            prev_key INTEGER, next_key INTEGER,
            current_page INTEGER NOT NULL, created_at INTEGER NOT NULL
            )
        """.trimIndent())
    }
}