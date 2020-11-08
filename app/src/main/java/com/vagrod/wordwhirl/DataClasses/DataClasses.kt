package com.vagrod.wordwhirl.DataClasses

data class GroupsData (val groups : MutableList<GroupData> = mutableListOf())

data class GroupData(val id: String?, val name: String?, val wordsCount: Int, val isActive: Boolean)

data class GroupWords(val words: MutableList<WordsPair>?)

data class WordsPair(val first : String, val second : String, val noflip: Boolean = false)

data class WhirlOptions(val loop: Boolean, val flipAll: Boolean, val flipOnRandom: Boolean, val randomize: Boolean, val lastN : Int)

data class GroupFullContract(val group: GroupData, val data: GroupWords)

data class PlaylistPair(val originalPair: WordsPair, val first: String, val second: String, val isFlipped: Boolean)

data class UpdatedPairInfo(val original: WordsPair, val new: WordsPair)

data class WordsChangeset(val added: List<WordsPair>, val removed: List<WordsPair>, val updated: List<UpdatedPairInfo>)

data class SearchResult(val groupId: String, val groupName: String, val found: List<String>)

data class AllGroupsContract(val groups: MutableList<GroupFullContract>)