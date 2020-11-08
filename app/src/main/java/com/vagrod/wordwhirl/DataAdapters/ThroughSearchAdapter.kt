package com.vagrod.wordwhirl.DataAdapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import com.vagrod.wordwhirl.DataClasses.GroupWords
import com.vagrod.wordwhirl.DataClasses.SearchResult
import com.vagrod.wordwhirl.R
import com.vagrod.wordwhirl.DataClasses.UpdatedPairInfo
import com.vagrod.wordwhirl.DataClasses.WordsPair
import java.util.HashMap

class ThroughSearchAdapter(context: Context, private val query: String, dataAdapter: DataAdapter, searchInactive: Boolean, private val onClick: (groupId: String, query: String) -> Unit) : BaseAdapter() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private var displayedDataSource : List<SearchResult> = listOf()

    init {
        displayedDataSource = dataAdapter.searchThrough(query, searchInactive)

        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return displayedDataSource.size
    }

    override fun getItem(position: Int): Any {
        return displayedDataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val rowView = inflater.inflate(R.layout.list_item_search_result, parent, false)

        val groupName = rowView.findViewById(R.id.group_name) as TextView
        val wordsFound = rowView.findViewById(R.id.words_found) as TextView
        val data = getItem(position) as SearchResult

        groupName.text = data.groupName

        var words = ""

        data.found.forEach {
            words += it + "\n"
        }

        wordsFound.text = words

        rowView.setOnClickListener{
            onClick.invoke(data.groupId, query)
        }

        return rowView
    }

}