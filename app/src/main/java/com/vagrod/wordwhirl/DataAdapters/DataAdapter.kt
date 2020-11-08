package com.vagrod.wordwhirl.DataAdapters

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import com.beust.klaxon.Klaxon
import com.vagrod.wordwhirl.*
import com.vagrod.wordwhirl.DataAccess.*
import com.vagrod.wordwhirl.DataClasses.*

class DataAdapter(private val act: Activity) {

    private lateinit var db: AppDatabase
    private lateinit var groupsDao: GroupsDao
    private lateinit var wordsDao: WordsDao

    private val optionsFileName = "com.vagrod.wordwhirl.options"
    private val prefsDarkMode = "darkMode"
    private val prefsShowInactive = "showInactive"
    private val prefsSearchInactive = "searchInactive"

    var groupsData : GroupsData? = null

    private var prefsOptions : SharedPreferences

    init {
        val o = AppDatabase.getInstance(act)

        if (o != null)
        {
            db = o
            groupsDao = db.groupsDao()
            wordsDao = db.wordsDao()
        }

        prefsOptions = act.getSharedPreferences(optionsFileName, Context.MODE_PRIVATE)
    }

    fun readAllGroups(): GroupsData? {
        val res = GroupsData()
        val data = groupsDao.readAllGroups()

        data.forEach {
            res.groups.add(
                GroupData(
                    it.groupId,
                    it.groupName,
                    it.wordsCount,
                    it.isActive == 1
                )
            )
        }

        groupsData = res

        return res
    }

    fun getGroupInfo(id:String?): GroupData? {
        readGroupsIfNeeded()

        return groupsData?.groups?.find { it.id == id }
    }

    fun getGroupWords(id:String?) : GroupWords? {
        if (id == null)
            return null

        if(!hasGroup(id))
            return GroupWords(mutableListOf())

        val data = wordsDao.getGroupWords(id)
        val res = GroupWords(mutableListOf())

        data.forEach {
            res.words?.add(WordsPair(it.firstWord, it.secondWord, it.isNoFlip))
        }

        return res
    }

    fun getRandomGroupWords(n:Int) : GroupWords? {
        if (n <= 0)
            return null

        val data = wordsDao.getRandomGroupWords(n)
        val res = GroupWords(mutableListOf())
        val ht: HashSet<Int> = hashSetOf()

        data.forEach {
            val hash = (it.firstWord.trim() + "~|~" + it.secondWord.trim()).hashCode()

            if(!ht.contains(hash)) {
                ht.add(hash)
                res.words?.add(WordsPair(it.firstWord, it.secondWord, it.isNoFlip))
            }
        }

        return res
    }

    fun hasGroup(id: String?) : Boolean {
        readGroupsIfNeeded()

        return groupsData?.groups?.any{it.id == id} == true
    }

    fun setGroupActive(id: String?, isActive : Boolean) {
        readGroupsIfNeeded()

        if (id == null)
            return

        groupsDao.updateGroupIsActive(id, if(isActive) 1 else 0)

        // Update the cache
        val g = groupsData!!.groups.find { it.id == id }

        if(g != null){
           groupsData!!.groups.remove(g)
           groupsData!!.groups.add(GroupData(g.id,g.name,g.wordsCount,isActive))
        }
    }

    fun updateGroup(id: String?, name: String?, changeset : WordsChangeset?) {
        readGroupsIfNeeded()

        if (id == null)
            return

        if(groupsData?.groups?.any{it.id == id} == false){
            return // Nothing to update
        } else {
            if (name != null){
                groupsDao.updateGroupName(id, name)
            }

            if (changeset != null){
                val wc = wordsDao.getWordsCount(id)

                changeset.added.forEach {
                    wordsDao.addWord(WordsPairEntity(0, id, it.first, it.second, it.noflip))
                }
                changeset.updated.forEach {
                    wordsDao.updateWord(id, it.original.first, it.original.second, it.new.first, it.new.second, it.new.noflip)
                }
                changeset.removed.forEach {
                    wordsDao.deleteWord(id, it.first, it.second)
                }

                groupsDao.updateGroupWordsCount(id, wc + changeset.added.count() - changeset.removed.count())
            }
        }

        readAllGroups()

        Toast.makeText(act, act.getString(R.string.group_saved), Toast.LENGTH_SHORT).show()
    }

    fun addOrImportGroup(id: String?, name: String?, isActive: Int?, words : MutableList<WordsPair>?) {
        readGroupsIfNeeded()

        if (id == null)
            return

        if(groupsData?.groups?.any{it.id == id} == false){
            if (name != null && words != null) {
                groupsDao.addGroupInfo(WordsGroupEntity(0, id, name, words.count(), isActive ?: 1))

                words.forEach {
                    wordsDao.addWord(WordsPairEntity(0, id, it.first, it.second, it.noflip))
                }
            }
        } else {
            if (name != null && words != null){
                groupsDao.deleteGroupInfo(id)
                groupsDao.addGroupInfo(WordsGroupEntity(0, id, name, words.count(), isActive ?: 1))
            } else {
                if (name != null){
                    groupsDao.updateGroupName(id, name)
                }

                if (words != null){
                    groupsDao.updateGroupWordsCount(id, words.count())
                }
            }

            if (words != null){
                wordsDao.deleteAllGroupWords(id)

                words.forEach {
                    wordsDao.addWord(WordsPairEntity(0, id, it.first, it.second, it.noflip))
                }
            }
        }

        readAllGroups()

        Toast.makeText(act, act.getString(R.string.group_saved), Toast.LENGTH_SHORT).show()
    }

    fun removeGroup(id: String?){
        readGroupsIfNeeded()

        if (id == null)
            return

        if (!hasGroup(id))
            return

        wordsDao.deleteAllGroupWords(id)
        groupsDao.deleteGroupInfo(id)

        Toast.makeText(act, act.getString(R.string.group_removed), Toast.LENGTH_SHORT).show()
    }

    fun serializeWords(words: GroupWords?) : String? {
        return Klaxon().toJsonString(words)
    }

    fun deserializeWords(wordsData: String) : GroupWords? {
        if(wordsData == "")
            return null

        return Klaxon().parse<GroupWords>(wordsData)
    }

    fun serializeWhirlOptions(options: WhirlOptions?) : String? {
        return Klaxon().toJsonString(options)
    }

    fun deserializeWhirlOptions(optionsData: String) : WhirlOptions? {
        if(optionsData == "")
            return null

        return Klaxon().parse<WhirlOptions>(optionsData)
    }

    fun getAllGroupsContract() : AllGroupsContract {
        readGroupsIfNeeded()

        val res = AllGroupsContract(mutableListOf())
        val allWords =  wordsDao.getAllWords()
        val grouped = allWords.groupBy { it.groupId }

        grouped.forEach { g ->
            val groupId = g.key

            if (hasGroup(groupId)) {
                val groupData = getGroupInfo(groupId)
                val entry = GroupFullContract(groupData!!, GroupWords(g.value.map { WordsPair(it.firstWord, it.secondWord, it.isNoFlip) }.toMutableList()))

                res.groups.add(entry)
            }
        }

        return res
    }

    fun getGroupFullContract(groupId: String) : GroupFullContract? {
        readGroupsIfNeeded()

        if (!hasGroup(groupId))
            return null

        val groupData = getGroupInfo(groupId)
        val groupWords = getGroupWords(groupId)

        if(groupData == null || groupWords?.words == null)
            return null

        return GroupFullContract(groupData, groupWords)
    }

    fun serializeFullContract(groupData: GroupFullContract) : String {
        return Klaxon().toJsonString(groupData)
    }

    fun deserializeFullContract(groupData: String) : GroupFullContract? {
        if(groupData == "")
            return null

        return Klaxon().parse<GroupFullContract>(groupData)
    }

    fun serializeChangeset(changeset: WordsChangeset): String? {
        return Klaxon().toJsonString(changeset)
    }

    fun deserializeChangeset(csData: String): WordsChangeset? {
        if(csData == "")
            return null

        return Klaxon().parse<WordsChangeset>(csData)
    }

    fun serializeAllGroups(data: AllGroupsContract): String {
        return Klaxon().toJsonString(data)
    }

    fun deserializeAllGroups(s: String): AllGroupsContract? {
        if(s == "")
            return null

        return Klaxon().parse<AllGroupsContract>(s)
    }


    fun updateWordsPair(groupId: String, pair: WordsPair, newPair: WordsPair){
        if (!hasGroup(groupId))
            return

        val words = getGroupWords(groupId) ?: return

        val origPair = words.words?.find { it.first == pair.first && it.second == pair.second } ?: return

        wordsDao.updateWord(groupId, origPair.first, origPair.second, newPair.first, newPair.second, newPair.noflip)

        Toast.makeText(act, act.getString(R.string.word_updated), Toast.LENGTH_SHORT).show()
    }

    private fun readGroupsIfNeeded(){
        if (groupsData == null)
            readAllGroups()

        if (groupsData == null)
            groupsData = GroupsData(mutableListOf())
    }

    fun getIsDarkMode() : Boolean {
        return prefsOptions.getBoolean(prefsDarkMode, false)
    }

    fun setIsDarkMode(newValue: Boolean) {
        return prefsOptions.edit().putBoolean(prefsDarkMode, newValue).apply()
    }

    fun getSearchInactive() : Boolean {
        return prefsOptions.getBoolean(prefsSearchInactive, false)
    }

    fun setSearchInactive(newValue: Boolean) {
        return prefsOptions.edit().putBoolean(prefsSearchInactive, newValue).apply()
    }

    fun getShowInactive() : Boolean {
        return prefsOptions.getBoolean(prefsShowInactive, false)
    }

    fun setShowInactive(newValue: Boolean) {
        return prefsOptions.edit().putBoolean(prefsShowInactive, newValue).apply()
    }

    fun getGroupWordsPortion(groupId: String?, skip: Int, take: Int): MutableList<WordsPair>? {
        readGroupsIfNeeded()

        if (groupId == null)
            return mutableListOf()

        if (!hasGroup(groupId))
            return mutableListOf()

        return wordsDao.getWordsPortion(groupId, skip, take).map { WordsPair(it.firstWord, it.secondWord, it.isNoFlip) }.toMutableList()
    }

    fun getGroupWordsPortionDesc(groupId: String?, skip: Int, take: Int): MutableList<WordsPair>? {
        readGroupsIfNeeded()

        if (groupId == null)
            return mutableListOf()

        if (!hasGroup(groupId))
            return mutableListOf()

        return wordsDao.getWordsPortionDesc(groupId, skip, take).map { WordsPair(it.firstWord, it.secondWord, it.isNoFlip) }.toMutableList()
    }

    fun getGroupWordsFiltered(groupId: String?, filter: String): MutableList<WordsPair>? {
        readGroupsIfNeeded()

        if (groupId == null)
            return mutableListOf()

        if (!hasGroup(groupId))
            return mutableListOf()

        val data1 = wordsDao.getWordsFiltered1(groupId, filter)
        val data2 = wordsDao.getWordsFiltered2(groupId, filter)
        return data1.union(data2).map { WordsPair(it.firstWord, it.secondWord, it.isNoFlip) }.toMutableList()
    }

    fun searchThrough(query: String, searchInactive: Boolean) : MutableList<SearchResult>{
        readGroupsIfNeeded()

        val data1 = if(searchInactive) wordsDao.searchThrough1(query) else wordsDao.searchThroughActive1(query)
        val data2 = if(searchInactive) wordsDao.searchThrough2(query) else wordsDao.searchThroughActive2(query)

        val u = (data1.union(data2)).groupBy { it.groupId }

        val r = u.map {
            val groupData = getGroupInfo(it.key)
            val words : MutableList<String> = mutableListOf()

            it.value.forEach {
                    w ->
                if(w.firstWord.contains(query, true))
                    if(!words.contains(w.firstWord)) words.add(w.firstWord)

                if(w.secondWord.contains(query, true))
                    if(!words.contains(w.secondWord)) words.add(w.secondWord)
            }

            SearchResult(it.key, groupData!!.name!!, words)
        }.toMutableList()

        r.sortBy { it.groupName }

        return  r
    }

    fun clearDatabase() {
        wordsDao.clear()
        groupsDao.clear()
    }

}