package com.vagrod.wordwhirl.DataAdapters

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.vagrod.wordwhirl.DataClasses.GroupWords
import com.vagrod.wordwhirl.R
import com.vagrod.wordwhirl.DataClasses.UpdatedPairInfo
import com.vagrod.wordwhirl.DataClasses.WordsPair
import java.util.HashMap

class DictionaryAdapter(private val context: Context, private val groupId: String, private val dataAdapter: DataAdapter, private val query: String?) : BaseAdapter() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private var displayedDataSource : GroupWords? = null

    private val viewMap: MutableMap<String, View> = HashMap()
    private val hiddenMap: MutableMap<String, Boolean> = HashMap()

    private val take = 20
    private var skip = 0

    private var isFiltered : Boolean = false
    private var globalHiddenFlag: Boolean = false

    init {
        displayedDataSource = GroupWords(mutableListOf())

        setUpVisualItems()

        notifyDataSetChanged()
    }

    private fun setUpVisualItems(){
        if(!isFiltered){
            val newPortion = dataAdapter.getGroupWordsPortionDesc(groupId, skip, take)

            if (newPortion?.count() ?: 0 == 0){
                skip -= take
                return
            }

            newPortion?.forEach {
                displayedDataSource?.words?.add(it)
            }
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
        val rowView = inflater.inflate(R.layout.list_item_dictionary, parent, false)

        val wordFirst = rowView.findViewById(R.id.word_first) as TextView
        val wordSecond = rowView.findViewById(R.id.word_second) as TextView
        val pair = getItem(position) as WordsPair

        if(query != null){
            if (pair.first.contains(query, true) || pair.second.contains(query, true) ){
                val bg = rowView.findViewById(R.id.dict_item_bg) as LinearLayout

                bg.background = ColorDrawable(context.resources.getColor( R.color.search_highlight ))
            }
        }

        wordFirst.text = pair.first

        if(!hiddenMap.containsKey(pair.first+pair.second))
            hiddenMap[pair.first+pair.second] = globalHiddenFlag

        if (hiddenMap[pair.first+pair.second] == false) {
            wordSecond.text = pair.second
        } else {
            wordSecond.text = "********"
        }

        rowView.setOnClickListener{
            if (!globalHiddenFlag)
                return@setOnClickListener

            val h = hiddenMap[pair.first+pair.second]!!

            alterVisibilitySingle(pair, !h)
        }

        viewMap[pair.first+pair.second] = rowView

        return rowView
    }

    fun alterVisibilitySingle(it: WordsPair, isHidden: Boolean){
        val tv = viewMap[it.first+it.second]?.findViewById(R.id.word_second) as TextView?

        if(isHidden)
            tv?.text = "********"
        else
            tv?.text = it.second

        hiddenMap[it.first+it.second] = isHidden
    }

    fun alterVisibility(isHidden: Boolean){
        globalHiddenFlag = isHidden

        displayedDataSource?.words?.forEach {
            alterVisibilitySingle(it, isHidden)
        }
    }

    fun filter(s: String){
        isFiltered = true

        viewMap.clear()
        displayedDataSource = GroupWords(dataAdapter.getGroupWordsFiltered(groupId, s))

        notifyDataSetChanged()
    }

    fun resetFilter(){
        skip = 0
        isFiltered = false

        viewMap.clear()
        displayedDataSource?.words?.clear()

        setUpVisualItems()
        notifyDataSetChanged()
    }

    fun showNext(){
        if (isFiltered)
            return

        skip += take
        isFiltered = false

        setUpVisualItems()
        notifyDataSetChanged()
    }

}