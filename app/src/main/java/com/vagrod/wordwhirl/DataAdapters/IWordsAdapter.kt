package com.vagrod.wordwhirl.DataAdapters

import com.vagrod.wordwhirl.DataClasses.WordsPair

interface IWordsAdapter{
    fun showNext()
    fun addItem(p: WordsPair)
    fun updateItem(pair: WordsPair, newPair: WordsPair)
    fun filter(s: String)
    fun resetFilter()
}