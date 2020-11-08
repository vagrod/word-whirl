package com.vagrod.wordwhirl.Activities

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.content.Intent
import android.os.Handler
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuItemCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vagrod.wordwhirl.*
import com.vagrod.wordwhirl.DataAdapters.DataAdapter
import com.vagrod.wordwhirl.DataAdapters.IWordsAdapter
import com.vagrod.wordwhirl.DataAdapters.WordsPairAdapter
import com.vagrod.wordwhirl.DataAdapters.WordsPairInMemoryAdapter
import com.vagrod.wordwhirl.DataClasses.GroupWords
import com.vagrod.wordwhirl.DataClasses.UpdatedPairInfo
import com.vagrod.wordwhirl.DataClasses.WordsChangeset
import com.vagrod.wordwhirl.DataClasses.WordsPair

class WordsEditorActivity : AppCompatActivity() {

    private var wordsData : GroupWords? = null
    private lateinit var listView : ListView
    private lateinit var addButton : FloatingActionButton
    private lateinit var adapter: IWordsAdapter
    private lateinit var dataAdapter : DataAdapter

    private var added: MutableList<WordsPair> = mutableListOf()
    private var removed: MutableList<WordsPair> = mutableListOf()
    private var updated: MutableList<UpdatedPairInfo> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_words_editor)

        setTitle(R.string.title_activity_words_editor)

        val mode = intent.getIntExtra("mode", MODE_Create)
        val groupId = intent.getStringExtra("groupId") ?: ""

        if(groupId == "")
            return

        dataAdapter = DataAdapter(this)

        if (mode == MODE_Create) {
            val wordsJson = intent.getStringExtra("wordsData") ?: ""

            wordsData = dataAdapter.deserializeWords(wordsJson)
        }

        listView = findViewById(R.id.words_list_view)

        if (mode == MODE_Create) {
            adapter = WordsPairInMemoryAdapter(this, wordsData,
                { p ->
                    onItemRemoved(p)
                }, { p ->
                    onItemEditRequest(p)
                })
        } else {
            adapter = WordsPairAdapter(this, groupId, dataAdapter,
                { p ->
                    onItemRemoved(p)
                }, { p ->
                    onItemEditRequest(p)
                })
        }

        listView.adapter = adapter as BaseAdapter

        addButton = findViewById(R.id.word_add)
        addButton.setOnClickListener{
            addOrEditWordPair(null)
        }

        listView.setOnScrollListener(object: AbsListView.OnScrollListener
        {
            private var preLast: Int = -1

            override fun onScroll(list: AbsListView?, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                if (list?.id == R.id.words_list_view){
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

    private fun onItemEditRequest(p: WordsPair) {
        addOrEditWordPair(p)
    }

    private fun onItemRemoved(p: WordsPair){
        val addedItem = added.find { it.first == p.first && it.second == p.second }
        val updatedItem = updated.find { it.original.first == p.first && it.original.second == p.second }

        if (updatedItem != null) {
            val ind = updated.indexOf(updatedItem)
            updated.removeAt(ind)
        }

        if (addedItem == null)
            removed.add(p)
        else {
            val ind = added.indexOf(addedItem)
            added.removeAt(ind)
        }
    }

    private fun addOrEditWordPair(pair: WordsPair?) {
        val builder = AlertDialog.Builder(this)
        val inflater: LayoutInflater = this.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dialogView = inflater.inflate(R.layout.word_pair_editor, null)
        val wordFirst = dialogView.findViewById<EditText>(R.id.wordeditor_first)
        val wordSecond = dialogView.findViewById<EditText>(R.id.wordeditor_second)
        val noflip = dialogView.findViewById<CheckBox>(R.id.noflip)

        if(pair != null){
            wordFirst?.setText(pair.first)
            wordSecond?.setText(pair.second)
            noflip?.isChecked = pair.noflip
        }

        builder.setTitle(R.string.wordpair_title)
        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            if (pair == null) {
                val newPair = WordsPair(wordFirst.text.toString(), wordSecond.text.toString(), noflip.isChecked)

                added.add(newPair)
                adapter.addItem(newPair)
            } else {
                val newPair = WordsPair(wordFirst.text.toString(), wordSecond.text.toString(), noflip.isChecked)

                updated.add(UpdatedPairInfo(pair, newPair))
                adapter.updateItem(pair, newPair)
            }
        }
        builder.setNegativeButton(android.R.string.cancel) { _, _ -> }

        builder.setView(dialogView)

        val b = builder.create()

        b.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_save -> {
                val newIntent = Intent()
                newIntent.putExtra("changeset", dataAdapter.serializeChangeset(WordsChangeset(added, removed, updated)))
                setResult(Activity.RESULT_OK, newIntent)

                this.finish()
                return true
            }
            R.id.action_cancel -> {
                setResult(Activity.RESULT_CANCELED)
                this.finish()
                return true
            }
        }

        return false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.words_list_actions, menu)

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
