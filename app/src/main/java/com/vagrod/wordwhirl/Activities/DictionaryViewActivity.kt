package com.vagrod.wordwhirl.Activities

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.AbsListView
import android.widget.ListView
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuItemCompat
import com.vagrod.wordwhirl.DataAdapters.DataAdapter
import com.vagrod.wordwhirl.DataAdapters.DictionaryAdapter
import com.vagrod.wordwhirl.R

import kotlinx.android.synthetic.main.activity_dictionary_view.*

class DictionaryViewActivity : AppCompatActivity() {

    private lateinit var dataAdapter : DataAdapter
    private lateinit var listView : ListView
    private lateinit var adapter: DictionaryAdapter

    private var isHidden: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dictionary_view)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val groupId = intent.getStringExtra("groupId") ?: ""
        val query = intent.getStringExtra("query")

        if(groupId == "")
            return

        dataAdapter = DataAdapter(this)

        title = getString(R.string.title_activity_dictionary) + " — " + dataAdapter.getGroupInfo(groupId)?.name

        listView = findViewById(R.id.words_dict_view)
        adapter = DictionaryAdapter(this, groupId, dataAdapter, query)
        listView.adapter = adapter

        listView.setOnScrollListener(object: AbsListView.OnScrollListener
        {
            private var preLast: Int = -1

            override fun onScroll(list: AbsListView?, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                if (list?.id == R.id.words_dict_view){
                    val lastItem = firstVisibleItem + visibleItemCount

                    if(lastItem == totalItemCount)
                    {
                        if(preLast != lastItem)
                        {
                            preLast = lastItem
                            adapter.showNext()
                        }
                    }
                }
            }

            override fun onScrollStateChanged(view: AbsListView?, state: Int) {

            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_showhide -> {
                isHidden = !isHidden

                if (isHidden)
                    item.icon = getDrawable(R.drawable.ic_show_white_24dp)
                else
                    item.icon = getDrawable(R.drawable.ic_hide_white_24dp)

                adapter.alterVisibility(isHidden)

                return true
            }
        }

        return false
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.dictionary_actions, menu)

        val search = menu.findItem(R.id.search).actionView as SearchView

        search.isIconifiedByDefault = false

        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(s: String): Boolean {
                adapter.filter(s)
                return true
            }

            override fun onQueryTextChange(s: String): Boolean {
                return true
            }
        })

        MenuItemCompat.setOnActionExpandListener (menu.findItem(R.id.search), object : MenuItemCompat.OnActionExpandListener {
            override fun onMenuItemActionExpand(item : MenuItem) : Boolean{
                Handler().post {
                    search.requestFocus()

                    val imm = getSystemService (Context.INPUT_METHOD_SERVICE) as InputMethodManager

                    imm.showSoftInput(search.findFocus(), 0)
                }

                return true
            }

            override fun onMenuItemActionCollapse(item : MenuItem) : Boolean{
                search.setQuery("", false)
                adapter.resetFilter()

                return true
            }
        })

        return true
    }

}
