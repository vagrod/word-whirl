package com.vagrod.wordwhirl.DataAdapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import com.vagrod.wordwhirl.DataClasses.GroupWords
import com.vagrod.wordwhirl.R
import com.vagrod.wordwhirl.DataClasses.WordsPair

class WordsPairInMemoryAdapter(context: Context, private val dataSource: GroupWords?, private val onRemove: (removedPair: WordsPair) -> Unit = {}, private val editRequest: (pair: WordsPair) -> Unit = {}) : BaseAdapter(), IWordsAdapter {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private var displayedDataSource : GroupWords? = null

    private var isFiltered : Boolean = false

    init {
        setUpVisualItems()

        notifyDataSetChanged()
    }

    private fun setUpVisualItems(){
        displayedDataSource = GroupWords(dataSource?.words?.toMutableList())
    }

    override fun getCount(): Int {
        return displayedDataSource?.words?.size ?: 0
    }

    override fun getItem(position: Int): Any {
        return displayedDataSource?.words?.get(position) ?: WordsPair("", "")
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val rowView = inflater.inflate(R.layout.list_item_word_pair, parent, false)

        val wordRemove = rowView.findViewById(R.id.word_remove) as Button
        val wordFirst = rowView.findViewById(R.id.word_first) as TextView
        val wordSecond = rowView.findViewById(R.id.word_second) as TextView

        val pair = getItem(position) as WordsPair

        wordFirst.text = pair.first
        wordSecond.text = pair.second
        wordRemove.setOnClickListener{ removeItem(pair) }
        rowView.setOnClickListener{ editRequest(pair) }

        return rowView
    }

    private fun getItemSourceIndex(pair : WordsPair) : Int {
        val orig = dataSource?.words?.find { it.first == pair.first && it.second == pair.second }

        return dataSource?.words?.indexOf(orig) ?: -1
    }

    private fun getItemDisplayIndex(pair : WordsPair) : Int {
        val disp = displayedDataSource?.words?.find { it.first == pair.first && it.second == pair.second }

        return displayedDataSource?.words?.indexOf(disp) ?: -1
    }

    override fun filter(s: String){
        val wd = dataSource?.words?.filter { it.first.contains(s, ignoreCase = true) || it.second.contains(s, ignoreCase = true) }?.toMutableList()

        displayedDataSource = GroupWords(wd)

        isFiltered = true

        notifyDataSetChanged()
    }

    override fun resetFilter(){
        setUpVisualItems()
        notifyDataSetChanged()

        isFiltered = false
    }

    override fun updateItem(pair: WordsPair, newPair: WordsPair){
        val sourceIndex = getItemSourceIndex(pair)
        val origPair = dataSource?.words?.get(sourceIndex)

        if(origPair == null)
            return

        val dispInd = getItemDisplayIndex(origPair)

        dataSource?.words?.removeAt(sourceIndex)
        displayedDataSource?.words?.removeAt(dispInd)

        dataSource?.words?.add(sourceIndex, newPair)
        displayedDataSource?.words?.add(dispInd, newPair)

        notifyDataSetChanged()
    }

    private fun removeItem(pair: WordsPair){
        val origInd = getItemSourceIndex(pair)
        val dispInd = getItemDisplayIndex(pair)

        if (origInd  > -1) {
            val removedItem = dataSource?.words?.get(origInd)

            dataSource?.words?.removeAt(origInd)
            displayedDataSource?.words?.removeAt(dispInd)

            @Suppress("NullChecksToSafeCall")
            if (removedItem != null){
                onRemove(removedItem)
            }

            notifyDataSetChanged()
        }
    }

    override fun addItem(pair: WordsPair){
        dataSource?.words?.add(pair)

        if(!isFiltered)
            displayedDataSource?.words?.add(pair)

        notifyDataSetChanged()
    }

    override fun showNext(){

    }

}