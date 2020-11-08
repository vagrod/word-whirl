package com.vagrod.wordwhirl.DataAccess

import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE

@Dao
interface GroupsDao {

    @Query("select * from Groups")
    fun readAllGroups(): List<WordsGroupEntity>

    @Query("select * from Groups where uid = :groupId")
    fun getGroupInfo(groupId: String): WordsGroupEntity

    @Insert(onConflict = REPLACE)
    fun addGroupInfo(group: WordsGroupEntity)

    @Query("update Groups set groupName = :name where groupId = :groupId")
    fun updateGroupName(groupId: String, name: String)

    @Query("update Groups set isActive = :active where groupId = :groupId")
    fun updateGroupIsActive(groupId: String, active: Int)

    @Query("update Groups set wordsCount = :wordsCount where groupId = :groupId")
    fun updateGroupWordsCount(groupId: String, wordsCount: Int)

    @Query("delete from Groups where groupId = :groupId")
    fun deleteGroupInfo(groupId: String)

    @Query("delete from Groups")
    fun clear()
}