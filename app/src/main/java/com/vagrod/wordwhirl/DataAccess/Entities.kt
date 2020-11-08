package com.vagrod.wordwhirl.DataAccess

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Groups")
data class WordsGroupEntity (
    @PrimaryKey(autoGenerate = true) val uid: Int,
    @ColumnInfo(name = "groupId") val groupId: String,
    @ColumnInfo(name = "groupName") val groupName: String,
    @ColumnInfo(name = "wordsCount") val wordsCount: Int,
    @ColumnInfo(name = "isActive") val isActive: Int
)

@Entity(tableName = "Words")
data class WordsPairEntity (
    @PrimaryKey(autoGenerate = true) val uid: Int,
    @ColumnInfo(name = "groupId") val groupId: String,
    @ColumnInfo(name = "firstWord") val firstWord: String,
    @ColumnInfo(name = "secondWord") val secondWord: String,
    @ColumnInfo(name = "isNoFlip") val isNoFlip: Boolean
)