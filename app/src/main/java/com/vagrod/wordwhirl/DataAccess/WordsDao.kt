package com.vagrod.wordwhirl.DataAccess

import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.vagrod.wordwhirl.DataClasses.SearchResult

@Dao
interface WordsDao {

    @Query("select * from Words where groupId = :groupId")
    fun getGroupWords(groupId: String): List<WordsPairEntity>

    @Query("select w.* from Words w join Groups g on w.groupId = g.groupId where g.isActive = 1 order by random() limit :n ")
    fun getRandomGroupWords(n: Int): List<WordsPairEntity>

    @Insert(onConflict = REPLACE)
    fun addWord(wordsPair: WordsPairEntity)

    @Query("delete from Words where groupId = :groupId")
    fun deleteAllGroupWords(groupId: String)

    @Query("update Words set firstWord=:newFirstWord, secondWord = :newSecondWord, isNoFlip = :newNoFlip where groupId = :groupId and firstWord = :firstWord and secondWord = :secondWord")
    fun updateWord(groupId: String, firstWord: String, secondWord: String, newFirstWord: String, newSecondWord: String, newNoFlip: Boolean)

    @Query("delete from Words where groupId = :groupId and firstWord = :firstWord and secondWord = :secondWord")
    fun deleteWord(groupId: String, firstWord: String, secondWord: String)

    @Query("select COUNT(uid) from Words where groupId = :groupId")
    fun getWordsCount(groupId: String) : Int

    @Query("select * from Words where groupId = :groupId limit :take offset :skip")
    fun getWordsPortion(groupId: String, skip: Int, take: Int): List<WordsPairEntity>

    @Query("select * from (select * from Words order by uid desc) where groupId = :groupId limit :take offset :skip")
    fun getWordsPortionDesc(groupId: String, skip: Int, take: Int): List<WordsPairEntity>

    @Query("select * from Words where groupId = :groupId and (firstWord like '%' || :filter || '%')")
    fun getWordsFiltered1(groupId: String, filter: String): List<WordsPairEntity>

    @Query("select * from Words where groupId = :groupId and (secondWord like '%' || :filter || '%')")
    fun getWordsFiltered2(groupId: String, filter: String): List<WordsPairEntity>

    @Query("select * from Words where (firstWord like '%' || :query || '%')")
    fun searchThrough1(query: String): List<WordsPairEntity>

    @Query("select * from Words where (secondWord like '%' || :query || '%')")
    fun searchThrough2(query: String): List<WordsPairEntity>

    @Query("select w.* from Words w join Groups g on g.groupId = w.groupId where (w.firstWord like '%' || :query || '%') and g.isActive = 1")
    fun searchThroughActive1(query: String): List<WordsPairEntity>

    @Query("select w.* from Words w join Groups g on g.groupId = w.groupId where (w.secondWord like '%' || :query || '%') and g.isActive = 1")
    fun searchThroughActive2(query: String): List<WordsPairEntity>

    @Query("select * from Words")
    fun getAllWords(): List<WordsPairEntity>

    @Query("delete from Words")
    fun clear()

}