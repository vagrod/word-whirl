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
import com.vagrod.wordwhirl.DataClasses.UpdatedPairInfo
import com.vagrod.wordwhirl.DataClasses.WordsPair

class WordsPairAdapter(context: Context, private val groupId: String, private val dataAdapter: DataAdapter, private val onRemove: (removedPair: WordsPair) -> Unit = {}, private val editRequest: (pair: WordsPair) -> Unit = {}) : BaseAdapter(), IWordsAdapter {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private var displayedDataSource : GroupWords? = null

    private val take = 20
    private var skip = 0

    private val updated: MutableList<UpdatedPairInfo> = mutableListOf()
    private val removed: MutableList<WordsPair> = mutableListOf()
    private val added: MutableList<WordsPair> = mutableListOf()

    private var isFiltered : Boolean = false

    init {
        displayedDataSource = GroupWords(mutableListOf())

        setUpVisualItems()

        notifyDataSetChanged()
    }

    private fun setUpVisualItems(){
        if(!isFiltered){
            val newPortion = dataAdapter.getGroupWordsPortion(groupId, skip, take)

            if (newPortion?.count() ?: 0 == 0){
                skip -= take
                return
            }

            added.forEach {
                val ind = displayedDataSource?.words?.indexOf(it) ?: -1

                if (ind > -1){
                    displayedDataSource?.words?.removeAt(ind)
                }
            }

            newPortion?.forEach {
                displayedDataSource?.words?.add(it)
            }

            alterDataSource()
        }
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

    private fun getItemDisplayIndex(pair : WordsPair) : Int {
        val disp = displayedDataSource?.words?.find { it.first == pair.first && it.second == pair.second }

        return displayedDataSource?.words?.indexOf(disp) ?: -1
    }

    override fun filter(s: String){
        isFiltered = true

        displayedDataSource = GroupWords(dataAdapter.getGroupWordsFiltered(groupId, s))

        alterDataSource(s)

        notifyDataSetChanged()
    }

    private fun alterDataSource(filter: String? = null){
        added.forEach {
            if (filter == null || (it.first.toLowerCase().contains(filter.toLowerCase()) || it.second.toLowerCase().contains(filter.toLowerCase())))
                displayedDataSource?.words?.add(it)
        }

        updated.forEach {
            val item = displayedDataSource?.words?.find { w -> w.first == it.original.first && w.second == it.original.second }

            if (item != null){
                val ind = displayedDataSource?.words?.indexOf(item) ?: -1

                if (ind > -1){
                    displayedDataSource?.words?.removeAt(ind)
                    displayedDataSource?.words?.add(ind, it.new)
                }
            } else {
                if (filter == null || (it.new.first.toLowerCase().contains(filter.toLowerCase()) || it.new.second.toLowerCase().contains(filter.toLowerCase()))) {
                    if (displayedDataSource?.words?.any { w -> w.first == it.new.first && w.second == it.new.second} == false)
                        displayedDataSource?.words?.add(it.new)
                }
            }
        }

        removed.forEach {
            val item = displayedDataSource?.words?.find { w -> w.first == it.first && w.second == it.second }

            if (item != null){
                val ind = displayedDataSource?.words?.indexOf(item) ?: -1

                if (ind > -1){
                    displayedDataSource?.words?.removeAt(ind)
                }
            }
        }
    }

    override fun resetFilter(){
        skip = 0
        isFiltered = false

        displayedDataSource?.words?.clear()

        setUpVisualItems()

        notifyDataSetChanged()
    }

    override fun updateItem(pair: WordsPair, newPair: WordsPair){
        val dispInd = getItemDisplayIndex(pair)

        val item = updated.find { it.new.first == pair.first && it.new.second == pair.second }

        if (item != null){
            val ind = updated.indexOf(item)

            if(ind > -1)
                updated.removeAt(ind)
        }

        updated.add(UpdatedPairInfo(pair, newPair))

        displayedDataSource?.words?.removeAt(dispInd)
        displayedDataSource?.words?.add(dispInd, newPair)

        notifyDataSetChanged()
    }

    private fun removeItem(pair: WordsPair){
        val dispInd = getItemDisplayIndex(pair)

        if (dispInd  > -1) {
            val removedItem = displayedDataSource?.words?.get(dispInd)

            removed.add(pair)

            displayedDataSource?.words?.removeAt(dispInd)

            @Suppress("NullChecksToSafeCall")
            if (removedItem != null){
                onRemove(removedItem)
            }

            notifyDataSetChanged()
        }
    }

    override fun addItem(pair: WordsPair){
        if(!isFiltered){
            displayedDataSource?.words?.add(pair)

            added.add(pair)

            notifyDataSetChanged()
        }
    }

    override fun showNext(){
        if (isFiltered)
            return

        skip += take
        isFiltered = false

        setUpVisualItems()
        notifyDataSetChanged()
    }

}