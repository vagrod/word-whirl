package com.vagrod.wordwhirl.Activities

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.MenuItemCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.vagrod.wordwhirl.DataAdapters.DataAdapter
import com.vagrod.wordwhirl.DataClasses.GroupData
import com.vagrod.wordwhirl.DataClasses.WhirlOptions
import com.vagrod.wordwhirl.Helpers.OnSwipeTouchListener
import com.vagrod.wordwhirl.R
import java.util.*


const val ID_NewGroup:Int = 50000 + 1
const val MODE_Create:Int = 1
const val MODE_Edit:Int = 2


class MainActivity : AppCompatActivity() {

    private lateinit var sideMenu : Menu
    private var topMenu : Menu? = null
    private lateinit var dataAdapter : DataAdapter
    private var selectedGroup: GroupData? = null
    private var groupsMenuItemsMapping : HashMap<MenuItem, String?> = hashMapOf()
    private lateinit var startSingleButton : Button
    private lateinit var startAllButton : Button
    private lateinit var addButton : ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)

        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)

        startSingleButton = findViewById(R.id.start_whirl)
        startAllButton = findViewById(R.id.start_whirl_global)
        addButton = findViewById(R.id.whirl_group_add)

        val card = findViewById<LinearLayout>(R.id.layout)

        card.setOnTouchListener(object : OnSwipeTouchListener(this) {
            override fun onSwipeLeft() {
                prevGroup()
            }
            override fun onSwipeRight() {
                nextGroup()
            }
        })

        findViewById<ImageView>(R.id.whirl_group_add).setOnClickListener{createNewGroup()}

        findViewById<CheckBox>(R.id.wrap_only_last_n).setOnCheckedChangeListener { _, value ->
            val edit = findViewById<EditText>(R.id.last_n_value)

            edit.isEnabled = value

            if(!value)
                edit.text = null
        }

        startSingleButton.setOnClickListener{startWhirlSingle()}
        startAllButton.setOnClickListener{startWhirlGlobal()}

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        dataAdapter = DataAdapter(this)

        if (dataAdapter.getIsDarkMode())
            AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)
        else
            AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)

        onDarkModeChanged()

        dataAdapter.readAllGroups()

        sideMenu = navView.menu

        navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                ID_NewGroup -> {
                    createNewGroup()
                    true
                }
                else -> {
                    val groupId = groupsMenuItemsMapping[it] ?: return@setNavigationItemSelectedListener true

                    val g = dataAdapter.getGroupInfo(groupId)

                    selectedGroup = g

                    refreshMainUI()

                    drawerLayout.closeDrawer(GravityCompat.START)

                    true
                }
            }
        }

        buildGroupsMenu(sideMenu)

        selectedGroup = getAvailableGroups().firstOrNull()

        refreshMainUI()
    }

    private fun getAvailableGroups() : MutableList<GroupData>{
        if(dataAdapter.groupsData?.groups == null)
            return mutableListOf()

        if(dataAdapter.getShowInactive())
            return dataAdapter.groupsData?.groups!!
        else
            return dataAdapter.groupsData?.groups!!.filter { it.isActive }.toMutableList()
    }

    private fun prevGroup(){
        if (dataAdapter.groupsData?.groups == null)
            return

        val g = getAvailableGroups()

        if (selectedGroup == null)
            selectedGroup = g.firstOrNull()

        var ind = g.indexOf(selectedGroup) ?: -1
        val cnt = g.count() ?: -1

        if (cnt < 0)
            return

        if (ind + 1 >= cnt)
            ind = -1

        if (ind + 1 < cnt)
            selectedGroup = g.get(ind + 1)

        refreshMainUI()
    }

    private fun nextGroup(){
        if (dataAdapter.groupsData?.groups == null)
            return

        val g = getAvailableGroups()

        if (selectedGroup == null)
            selectedGroup = g.firstOrNull()

        var ind = g.indexOf(selectedGroup) ?: -1
        val cnt = g.count() ?: -1

        if (cnt < 0)
            return

        if (ind - 1 < 0)
            ind = cnt

        selectedGroup = g.get(ind - 1)

        refreshMainUI()
    }

    private fun createNewGroup(){
        val newIntent = Intent(this, GroupSettingsActivity::class.java)
        newIntent.putExtra("mode", MODE_Create)
        newIntent.putExtra("groupId", UUID.randomUUID().toString())

        startActivityForResult(newIntent, 0)
    }

    private fun throughSearch(query: String){
        if(query.isEmpty())
            return

        val newIntent = Intent(this, ThroughSearchActivity::class.java)
        newIntent.putExtra("query", query)

        startActivityForResult(newIntent, 5)
    }

    private fun showSettings(){
        val newIntent = Intent(this, SettingsActivity::class.java)

        startActivityForResult(newIntent, 6)
    }

    private fun showDictionary(){
        if (selectedGroup == null)
            return

        val newIntent = Intent(this, DictionaryViewActivity::class.java)
        newIntent.putExtra("groupId", selectedGroup?.id)

        startActivityForResult(newIntent, 4)
    }

    private fun startWhirlSingle(){
        if (selectedGroup == null)
            return

        val lastN = findViewById<EditText>(R.id.last_n_value)
        var lastNValue : Int = -1
        val allowedWords : Int = selectedGroup?.wordsCount ?: 0

        if (findViewById<CheckBox>(R.id.wrap_only_last_n).isChecked){
            if (lastN.text.toString() == "")
                lastNValue = -1
            else {
                val value = lastN.text.toString().toInt()

                if (value == 0)
                    lastNValue = -1
                else {
                    if (value > allowedWords){
                        val builder = AlertDialog.Builder(this)

                        builder.setTitle(R.string.last_n_validation_title)
                        builder.setMessage(getString(R.string.last_n_validation_message) + " " + allowedWords.toString())
                        builder.setPositiveButton(android.R.string.ok) { _, _ -> }

                        builder.show()
                        return
                    } else
                        lastNValue = value
                }
            }
        }

        val options = WhirlOptions(
            findViewById<CheckBox>(R.id.loop_whirl).isChecked,
            findViewById<CheckBox>(R.id.flip_all_whirl).isChecked,
            findViewById<CheckBox>(R.id.flip_rnd_whirl).isChecked,
            findViewById<CheckBox>(R.id.rnd_whirl).isChecked,
            lastNValue
        )

        val newIntent = Intent(this, WhirlActivity::class.java)
        newIntent.putExtra("options", dataAdapter.serializeWhirlOptions(options))
        newIntent.putExtra("groupId", selectedGroup?.id)

        startActivityForResult(newIntent, 3)
    }

    private fun startWhirlGlobal(){
        val nText = findViewById<EditText>(R.id.last_n_value).text

        if(nText.isNullOrEmpty()){
            val builder = AlertDialog.Builder(this)

            builder.setTitle(R.string.last_n_validation_title)
            builder.setMessage(getString(R.string.last_n_global_is_null))
            builder.setPositiveButton(android.R.string.ok) { _, _ -> }

            builder.show()
            return
        }

        val lastNValue = findViewById<EditText>(R.id.last_n_value).text.toString().toInt()

        if (lastNValue <= 0){
            val builder = AlertDialog.Builder(this)

            builder.setTitle(R.string.last_n_validation_title)
            builder.setMessage(getString(R.string.last_n_global_validation_message))
            builder.setPositiveButton(android.R.string.ok) { _, _ -> }

            builder.show()
            return
        }

        val options = WhirlOptions(
            findViewById<CheckBox>(R.id.loop_whirl).isChecked,
            findViewById<CheckBox>(R.id.flip_all_whirl).isChecked,
            findViewById<CheckBox>(R.id.flip_rnd_whirl).isChecked,
            findViewById<CheckBox>(R.id.rnd_whirl).isChecked,
            lastNValue
        )

        val newIntent = Intent(this, WhirlActivity::class.java)
        newIntent.putExtra("options", dataAdapter.serializeWhirlOptions(options))
        newIntent.putExtra("groupId", WhirlActivity.GLOBAL_WHIRL)

        startActivityForResult(newIntent, 3)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            0 -> when (resultCode) {
                RESULT_OK -> {
                    buildGroupsMenu(sideMenu)
                    refreshMainUI()
                }
            }

            6 -> {
                when (resultCode) {
                    117 -> {
                        if(dataAdapter.getIsDarkMode()) {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        } else {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        }

                        onDarkModeChanged()
                    }
                }

                buildGroupsMenu(sideMenu)
                refreshMainUI()
            }

            111 -> when (resultCode) {
                RESULT_OK -> {
                    val uri = data?.data

                    if(uri == null)
                        return

                    val inputStream = contentResolver.openInputStream(uri)

                    if(inputStream == null)
                        return

                    val bytes = inputStream.readBytes()
                    val groupData = bytes.toString(Charsets.UTF_8)
                    val groupContract = dataAdapter.deserializeFullContract(groupData)

                    if (groupContract == null)
                        return

                    dataAdapter.addOrImportGroup(groupContract.group.id, groupContract.group.name, if(groupContract.group.isActive) 1 else 0, groupContract.data.words)

                    val builder = AlertDialog.Builder(this)

                    builder.setTitle(R.string.group_import_title)
                    builder.setMessage(getString(R.string.import_completed) + " \"" + groupContract.group.name + "\"")
                    builder.setPositiveButton(android.R.string.ok) { _, _ -> }

                    builder.show()

                    buildGroupsMenu(sideMenu)

                    selectedGroup = getAvailableGroups().firstOrNull()

                    refreshMainUI()
                }
            }
        }
    }

    private fun onDarkModeChanged(){
        if (dataAdapter.getIsDarkMode())
            addButton.setImageResource(R.drawable.add_dark_mode)
        else
            addButton.setImageResource(R.drawable.add_light_mode)
    }

    private fun refreshMainUI(){
        if (selectedGroup != null) {
            if (!dataAdapter.hasGroup(selectedGroup?.id)){
                selectedGroup = null
            }
        }

        val g = getAvailableGroups()

        if (selectedGroup == null){
            findViewById<LinearLayout>(R.id.whirl_options).visibility = View.GONE
            findViewById<LinearLayout>(R.id.no_whirl).visibility = View.VISIBLE

            if(g.count() ?: 0 > 0){
                findViewById<TextView>(R.id.whirl_hint).text = getString(R.string.whirl_not_selected)
            } else {
                findViewById<TextView>(R.id.whirl_hint).text = getString(R.string.whirl_no_whirl)
            }

            topMenu?.findItem(R.id.action_edit)?.isVisible = false
            title = getString(R.string.app_name)
        } else {
            findViewById<LinearLayout>(R.id.whirl_options).visibility = View.VISIBLE
            findViewById<LinearLayout>(R.id.no_whirl).visibility = View.GONE
            findViewById<TextView>(R.id.group_title).text = selectedGroup?.name

            topMenu?.findItem(R.id.action_edit)?.isVisible = true
            title = selectedGroup?.name
        }
    }

    private fun buildGroupsMenu(m: Menu) {
        dataAdapter.readAllGroups()

        m.clear()
        groupsMenuItemsMapping.clear()

        m.add(R.drawable.ic_add_white_24dp, ID_NewGroup, 0, R.string.menu_new_group)

        val g = getAvailableGroups()

        if (dataAdapter.groupsData != null) {
            var i = 0
            val a = g.toList()
            val l = a.sortedBy { it.name ?: "" }

            l.forEach {
                i++
                val s = SpannableString(it.name)
                val item = m.add(R.drawable.ic_receipt_white_24dp, ID_NewGroup + i, i, s)

                if(!it.isActive) {
                    if (dataAdapter.getIsDarkMode()) {
                        s.setSpan(ForegroundColorSpan(Color.LTGRAY), 0, s.length, 0)
                    } else {
                        s.setSpan(ForegroundColorSpan(Color.DKGRAY), 0, s.length, 0)
                    }
                }

                item.title = s

                val newGroupItem = m.findItem(ID_NewGroup + i)

                if(it.isActive) {
                    newGroupItem.icon = ContextCompat.getDrawable(this, R.drawable.ic_receipt_white_24dp)
                } else {
                    newGroupItem.icon = ContextCompat.getDrawable(this, R.drawable.ic_baseline_texture_24)
                }
                groupsMenuItemsMapping[newGroupItem] = it.id
            }
        }

        val newGroupItem = m.findItem(ID_NewGroup)
        newGroupItem.icon = ContextCompat.getDrawable(this, R.drawable.ic_add_white_24dp)
    }

    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        topMenu = menu
        menuInflater.inflate(R.menu.main, menu)

        val search = menu.findItem(R.id.through_search).actionView as SearchView

        search.isIconifiedByDefault = false

        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(s: String): Boolean {
                throughSearch(s)

                return true
            }

            override fun onQueryTextChange(s: String): Boolean {
                return true
            }
        })

        MenuItemCompat.setOnActionExpandListener (menu.findItem(R.id.through_search), object : MenuItemCompat.OnActionExpandListener {
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
                //adapter.resetFilter()

                return true
            }
        })

        menu.findItem(R.id.action_dictionary).isVisible = selectedGroup != null

        refreshMainUI()

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.open_settings -> {
                showSettings()

                true
            }
            R.id.action_dictionary -> {
                showDictionary()

                true
            }
            R.id.action_import -> {
                val intent = Intent()
                    .setType("*/*")
                    .setAction(Intent.ACTION_GET_CONTENT)

                startActivityForResult(Intent.createChooser(intent, getString(R.string.select_file)), 111)

                true
            }
            R.id.action_edit -> {
                val newIntent = Intent(this, GroupSettingsActivity::class.java)
                newIntent.putExtra("mode", MODE_Edit)
                newIntent.putExtra("groupId", selectedGroup?.id)

                startActivityForResult(newIntent, 0)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}
