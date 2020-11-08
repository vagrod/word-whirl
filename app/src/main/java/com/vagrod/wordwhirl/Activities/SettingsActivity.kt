package com.vagrod.wordwhirl.Activities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.vagrod.wordwhirl.DataAdapters.DataAdapter
import com.vagrod.wordwhirl.Helpers.DatabaseExporter
import com.vagrod.wordwhirl.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment(this, DataAdapter(this)))
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()

        return true
    }

    class SettingsFragment(private val parent: Activity, private val dataAdapter: DataAdapter) : PreferenceFragmentCompat() {

        private val PERMISSIONS_REQUEST_EXTERNAL_STORAGE : Int = 50

        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
            when (requestCode) {
                PERMISSIONS_REQUEST_EXTERNAL_STORAGE -> {
                    if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                        DatabaseExporter.exportAll(parent)
                    }
                    return
                }
                else -> {
                }
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)

            when (requestCode) {
                1112 -> when (resultCode) {
                    RESULT_OK -> {
                        val uri = data?.data

                        if(uri == null)
                            return

                        val inputStream = parent.contentResolver.openInputStream(uri)

                        if(inputStream == null)
                            return

                        val dataAdapter = DataAdapter(parent)
                        val bytes = inputStream.readBytes()
                        val groupData = bytes.toString(Charsets.UTF_8)
                        val allData = dataAdapter.deserializeAllGroups(groupData)

                        if (allData == null)
                            return

                        if(allData.groups.count() > 0){
                            dataAdapter.clearDatabase()

                            allData.groups.forEach{groupContract ->
                                dataAdapter.addOrImportGroup(groupContract.group.id, groupContract.group.name, if(groupContract.group.isActive) 1 else 0, groupContract.data.words)
                            }

                            val builder = AlertDialog.Builder(parent)

                            builder.setTitle(R.string.import_all_title)
                            builder.setMessage(getString(R.string.import_all_completed) + " " + allData.groups.count())
                            builder.setPositiveButton(android.R.string.ok) { _, _ -> }

                            builder.show()
                        } else {
                            val builder = AlertDialog.Builder(parent)

                            builder.setTitle(R.string.import_all_title)
                            builder.setMessage(getString(R.string.import_all_nodata))
                            builder.setPositiveButton(android.R.string.ok) { _, _ -> }

                            builder.show()
                        }
                    }
                }
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            // Version
            val prefVersion = findPreference<Preference>("pref_version")

            try {
                val pInfo = context?.packageManager?.getPackageInfo(context!!.packageName, 0)
                val version = pInfo?.versionName

                prefVersion!!.summary = version ?: "?"
            } catch (e: PackageManager.NameNotFoundException) {
                prefVersion!!.summary = "?"
                e.printStackTrace()
            }

            // Export all
            val prefExport = findPreference<Preference>("pref_export_all")
            prefExport?.onPreferenceClickListener = Preference.OnPreferenceClickListener { _ ->
                if (ContextCompat.checkSelfPermission(parent, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(parent, Manifest.permission.READ_CONTACTS)) {
                        val builder = AlertDialog.Builder(parent)

                        builder.setTitle(R.string.storage_permission_title)
                        builder.setMessage(R.string.storage_permission_message)
                        builder.setPositiveButton(android.R.string.ok) { _, _ -> }

                        builder.show()
                    } else {
                        ActivityCompat.requestPermissions(parent, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_EXTERNAL_STORAGE)
                    }
                } else {
                    DatabaseExporter.exportAll(parent)
                }
                true
            }

            // Import all
            val prefImport = findPreference<Preference>("pref_import_all")
            prefImport?.onPreferenceClickListener = Preference.OnPreferenceClickListener { _ ->
                val intent = Intent()
                    .setType("*/*")
                    .setAction(Intent.ACTION_GET_CONTENT)

                startActivityForResult(Intent.createChooser(intent, getString(R.string.select_file)), 1112)

                true
            }

            // Dark Mode
            val prefDm = findPreference<SwitchPreferenceCompat>("pref_dark_mode")

            prefDm?.isChecked = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
            prefDm?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, value ->
                val currentIsDark = value == true

                dataAdapter.setIsDarkMode(currentIsDark)

                parent.setResult(117)
                parent.finish()

                true
            }

            // Show Inactive
            val prefShow = findPreference<SwitchPreferenceCompat>("pref_show_inactive")

            prefShow?.isChecked = dataAdapter.getShowInactive()
            prefShow?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, value ->
                val currentValue = value == true

                dataAdapter.setShowInactive(currentValue)

                true
            }

            // Search Inactive
            val prefSearch = findPreference<SwitchPreferenceCompat>("pref_search_inactive")

            prefSearch?.isChecked = dataAdapter.getSearchInactive()
            prefSearch?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, value ->
                val currentValue = value == true

                dataAdapter.setSearchInactive(currentValue)

                true
            }
        }
    }
}