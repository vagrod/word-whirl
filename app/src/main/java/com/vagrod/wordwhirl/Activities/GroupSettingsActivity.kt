package com.vagrod.wordwhirl.Activities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.vagrod.wordwhirl.*
import com.vagrod.wordwhirl.DataAdapters.DataAdapter
import com.vagrod.wordwhirl.DataClasses.GroupData
import com.vagrod.wordwhirl.DataClasses.GroupWords
import com.vagrod.wordwhirl.DataClasses.WordsChangeset
import com.vagrod.wordwhirl.DataClasses.WordsPair
import com.vagrod.wordwhirl.Helpers.DatabaseExporter
import java.io.File

class GroupSettingsActivity : AppCompatActivity() {

    private val PERMISSIONS_REQUEST_EXTERNAL_STORAGE : Int = 50

    private lateinit var dataAdapter : DataAdapter
    private lateinit var fragment: SettingsFragment
    private var mainMenu : Menu? = null
    private var mode: Int = 0
    private var groupId : String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.group_settings_activity)

        mode = intent.getIntExtra("mode", 0)
        groupId = intent.getStringExtra("groupId")

        dataAdapter = DataAdapter(this)

        val groupName = dataAdapter.getGroupInfo(groupId)?.name

        fragment = SettingsFragment()
        fragment.mode = mode
        fragment.groupId = groupId
        fragment.groupName = groupName
        fragment.adapter = dataAdapter

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, fragment)
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (mode  == MODE_Edit) {
            setTitle(R.string.title_activity_group_settings_edit)
        }
        if (mode  == MODE_Create) {
            setTitle(R.string.title_activity_group_settings_new)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        mainMenu = menu
        menuInflater.inflate(R.menu.group_settings_actions, menu)

        if(mode == MODE_Create)
            mainMenu?.findItem(R.id.action_export)?.isVisible = false

        if(mode == MODE_Edit)
            mainMenu?.findItem(R.id.action_save)?.isVisible = false

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            16908332 -> {
                setResult(Activity.RESULT_CANCELED, Intent())

                this.finish()
                return true
            }
            R.id.action_save -> {
                if (mode == MODE_Create) {
                    val isValidWords = fragment.changeset != null && fragment.wordsDataForNew.words?.count() ?: 0 > 0

                    if (fragment.groupName == "" || fragment.groupName == null || !isValidWords) {
                        val builder = AlertDialog.Builder(this)

                        builder.setTitle(R.string.create_validation_title)
                        builder.setMessage(R.string.create_validation_message)
                        builder.setPositiveButton(android.R.string.ok) { _, _ -> }

                        builder.show()

                        return true
                    } else {
                        dataAdapter.addOrImportGroup(groupId, fragment.groupName, null, fragment.wordsDataForNew.words)

                        setResult(Activity.RESULT_OK, Intent())
                    }
                }

                this.finish()
                return true
            }
            R.id.action_export -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)) {
                        val builder = AlertDialog.Builder(this)

                        builder.setTitle(R.string.storage_permission_title)
                        builder.setMessage(R.string.storage_permission_message)
                        builder.setPositiveButton(android.R.string.ok) { _, _ -> }

                        builder.show()
                    } else {
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_EXTERNAL_STORAGE)
                    }
                } else {
                    DatabaseExporter.exportGroupData(this, groupId as  String)
                }
                return true
            }
        }

        return false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_EXTERNAL_STORAGE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    DatabaseExporter.exportGroupData(this, groupId as String)
                }
                return
            }
            else -> {
            }
        }
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED, Intent())

        this.finish()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        var mode : Int = 0
        var groupId : String? = ""
        var groupName : String? = ""
        var adapter : DataAdapter? = null
        var changeset: WordsChangeset? = null
        lateinit var wordsDataForNew: GroupWords

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.group_settings, rootKey)
        }
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            wordsDataForNew = GroupWords(mutableListOf())

            val groupInfo = adapter?.getGroupInfo(groupId)
            val wordsPref = findPreference<PreferenceScreen>("pref_words")

            wordsPref?.summary = """${getString(R.string.group_words_list_desc)} (${groupInfo?.wordsCount ?: 0})"""
            wordsPref?.setOnPreferenceClickListener{
                val newIntent = Intent(this.activity, WordsEditorActivity::class.java)

                newIntent.putExtra("groupId", groupId)
                newIntent.putExtra("mode", mode)

                if (mode == MODE_Create)
                    newIntent.putExtra("wordsData", adapter?.serializeWords(wordsDataForNew))

                startActivityForResult(newIntent, 1)

                true
            }

            val namePref = findPreference<EditTextPreference>("pref_name")

            namePref?.text = groupName

            namePref?.setOnPreferenceChangeListener { _, newValue ->
                if(mode == MODE_Edit)
                    adapter?.updateGroup(groupId, newValue as String?, null)

                groupName = newValue as String?

                true
            }

            val actPrefCat = findPreference<PreferenceCategory>("actions")
            if(mode == MODE_Create)
                actPrefCat?.isVisible = false

            val delPref = findPreference<PreferenceScreen>("pref_delete")

            delPref?.setOnPreferenceClickListener{
                val builder = AlertDialog.Builder(this.activity)
                builder.setTitle(R.string.remove_confirm_title)
                builder.setMessage(R.string.remove_confirm_message)

                builder.setPositiveButton(android.R.string.yes) { _, _ ->
                    if (mode == MODE_Edit)
                        adapter?.removeGroup(groupId)

                    activity?.setResult(Activity.RESULT_OK, Intent())

                    activity?.finish()
                }

                builder.setNegativeButton(android.R.string.no) { _, _ -> }

                builder.show()

                true
            }

            // Active
            val actPref = findPreference<PreferenceScreen>("pref_active")

            actPref?.setOnPreferenceClickListener{
                if(mode == MODE_Edit) {
                    val gi = adapter?.getGroupInfo(groupId)
                    val isActive = gi?.isActive == true

                    adapter?.setGroupActive(groupId, !isActive)

                    updateUI()
                }

                true
            }

            updateUI()
        }

        private fun updateUI(){
            val groupInfo = adapter?.getGroupInfo(groupId)
            val isActive = groupInfo?.isActive == true

            val namePref = findPreference<EditTextPreference>("pref_name")
            val actPref = findPreference<PreferenceScreen>("pref_active")

            actPref?.title = if (isActive) getString(R.string.group_set_inactive) else getString(R.string.group_set_active)
            actPref?.summary = if (isActive) getString(R.string.group_set_inactive_desc) else getString(R.string.group_set_active_desc)

            val activeString = if (groupInfo?.isActive == false) getString(R.string.group_inactive) else ""

            namePref?.title = """${getString(R.string.group_name)} $activeString"""
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            when (requestCode) {
                1 -> when (resultCode) {
                    RESULT_OK -> {
                        val csJson = data?.getStringExtra("changeset")

                        changeset = adapter?.deserializeChangeset(csJson  ?: "")

                        val wordsPref = findPreference<PreferenceScreen>("pref_words")
                        val wordsCount: Int

                        if (mode == MODE_Edit){
                            if(changeset?.added?.count() ?:0 == 0 && changeset?.removed?.count() ?:0 == 0 && changeset?.updated?.count() ?:0 == 0)
                                return

                            adapter?.updateGroup(groupId, null, changeset)

                            wordsCount = adapter?.getGroupInfo(groupId)?.wordsCount ?: 0
                        } else {
                            changeset?.added?.forEach {
                                wordsDataForNew.words?.add(WordsPair(it.first, it.second, it.noflip))
                            }
                            changeset?.updated?.forEach {
                                val e = wordsDataForNew.words?.find { w -> w.first == it.original.first && w.second == it.original.second }

                                if(e != null)
                                {
                                    val ind =  wordsDataForNew.words?.indexOf(e) ?: -1

                                    if (ind > -1){
                                        wordsDataForNew.words?.removeAt(ind)
                                        wordsDataForNew.words?.add(ind, it.new)
                                    }
                                }
                            }
                            changeset?.removed?.forEach {
                                val e = wordsDataForNew.words?.find { w -> w.first == it.first && w.second == it.second }

                                if(e != null)
                                {
                                    val ind =  wordsDataForNew.words?.indexOf(e) ?: -1

                                    if (ind > -1){
                                        wordsDataForNew.words?.removeAt(ind)
                                    }
                                }
                            }

                            wordsCount = wordsDataForNew.words?.count() ?: 0
                        }

                        wordsPref?.summary = """${getString(R.string.group_words_list_desc)} ($wordsCount)"""
                    }
                }
            }
        }
    }
}