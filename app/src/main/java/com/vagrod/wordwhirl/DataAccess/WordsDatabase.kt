package com.vagrod.wordwhirl.DataAccess

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [WordsGroupEntity::class, WordsPairEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun groupsDao(): GroupsDao
    abstract fun wordsDao(): WordsDao

    companion object {
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase? {
            if (INSTANCE == null) {
                synchronized(AppDatabase::class) {
                    val MIGRATION_1_2 = object : Migration(1, 2) {
                        override fun migrate(database: SupportSQLiteDatabase) {
                            database.execSQL("ALTER TABLE Groups ADD COLUMN isActive INTEGER DEFAULT 1 NOT NULL")
                        }
                    }

                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                        AppDatabase::class.java, "wordwhirl.words.db").addMigrations(MIGRATION_1_2)
                        .allowMainThreadQueries().build()
                }
            }
            return INSTANCE
        }
    }

}