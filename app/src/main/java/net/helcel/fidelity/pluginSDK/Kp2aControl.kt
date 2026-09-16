package net.helcel.fidelity.pluginSDK

import android.content.Intent
import org.json.JSONException
import org.json.JSONObject

object Kp2aControl {

    fun getAddEntryIntent(
        fields: HashMap<String, String>,
        protectedFields: ArrayList<String>?
    ): Intent {
        val outputData = JSONObject((fields as Map<*, *>)).toString()
        val startKp2aIntent = Intent(Strings.ACTION_START_WITH_TASK)
        startKp2aIntent.addCategory(Intent.CATEGORY_DEFAULT)
        startKp2aIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startKp2aIntent.putExtra("KP2A_APPTASK", "CreateEntryThenCloseTask")
        startKp2aIntent.putExtra("ShowUserNotifications", "true")
        startKp2aIntent.putExtra(Strings.EXTRA_ENTRY_OUTPUT_DATA, outputData)
        if (protectedFields != null)
            startKp2aIntent.putStringArrayListExtra(
                Strings.EXTRA_PROTECTED_FIELDS_LIST,
                protectedFields
            )
        return startKp2aIntent
    }

    fun getQueryEntryForOwnPackageIntent(): Intent {
        return Intent(Strings.ACTION_QUERY_CREDENTIALS_FOR_OWN_PACKAGE)
    }

    fun getEntryFieldsFromIntent(intent: Intent?): HashMap<String, String> {
        val res = HashMap<String, String>()
        try {
            val json = JSONObject(intent?.getStringExtra(Strings.EXTRA_ENTRY_OUTPUT_DATA) ?: "")
            val itr = json.keys()
            while (itr.hasNext()) {
                val key = itr.next()
                val value = json[key].toString()
                res[key] = value
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
        return res
    }
}
