package com.vagrod.wordwhirl.Helpers

import android.app.Activity
import android.app.AlertDialog
import android.os.Environment
import com.vagrod.wordwhirl.DataAdapters.DataAdapter
import com.vagrod.wordwhirl.R
import java.io.File

class DatabaseExporter {

    companion object {

        fun exportGroupData(activity: Activity, groupId: String) {
            val dataAdapter = DataAdapter(activity)
            val filename = groupId + ".json"
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename)
            val group = dataAdapter.getGroupFullContract(groupId) ?: return

            val data = dataAdapter.serializeFullContract(group)

            if (data == "")
                return

            file.writeText(data)

            val builder = AlertDialog.Builder(activity)

            builder.setTitle(R.string.export_success_title)
            builder.setMessage(activity.getString(R.string.export_success_message) + " \"" + filename + "\"")
            builder.setPositiveButton(android.R.string.ok) { _, _ -> }

            builder.show()
        }

        fun exportAll(activity: Activity){
            val dataAdapter = DataAdapter(activity)
            val filename = "WordWhirl.json"
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename)
            val dataObject = dataAdapter.getAllGroupsContract()

            val data = dataAdapter.serializeAllGroups(dataObject)

            if (data == "")
                return

            file.writeText(data)

            val builder = AlertDialog.Builder(activity)

            builder.setTitle(R.string.export_success_title)
            builder.setMessage(activity.getString(R.string.export_success_message) + " \"" + filename + "\"")
            builder.setPositiveButton(android.R.string.ok) { _, _ -> }

            builder.show()
        }

    }

}