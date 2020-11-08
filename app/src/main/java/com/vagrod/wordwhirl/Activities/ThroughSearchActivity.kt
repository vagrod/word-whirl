package com.vagrod.wordwhirl.Activities

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.ListView
import android.widget.SearchView
import androidx.core.view.MenuItemCompat
import com.vagrod.wordwhirl.DataAdapters.DataAdapter
import com.vagrod.wordwhirl.DataAdapters.DictionaryAdapter
import com.vagrod.wordwhirl.DataAdapters.ThroughSearchAdapter
import com.vagrod.wordwhirl.R
import kotlinx.android.synthetic.main.activity_through_search.*

class ThroughSearchActivity : AppCompatActivity() {

    private lateinit var dataAdapter : DataAdapter
    private lateinit var listView : ListView
    private lateinit var adapter: ThroughSearchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_through_search)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        dataAdapter = DataAdapter(this)

        val query = intent.getStringExtra("query") ?: ""

        title = getString(R.string.title_activity_through_search) + " — " + query

        listView = findViewById(R.id.search_results_view)
        adapter = ThroughSearchAdapter(this, query, dataAdapter, dataAdapter.getSearchInactive()){ groupId: String, query: String ->
            showDictionary(groupId, query)
        }

        listView.adapter = adapter
    }

    private fun showDictionary(groupId: String, query: String){
        val newIntent = Intent(this, DictionaryViewActivity::class.java)
        newIntent.putExtra("groupId", groupId)
        newIntent.putExtra("query", query)

        startActivityForResult(newIntent, 4)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.through_search_actions, menu)

        return true
    }

}