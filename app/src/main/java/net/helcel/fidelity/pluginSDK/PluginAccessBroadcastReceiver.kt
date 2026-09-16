package net.helcel.fidelity.pluginSDK

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PluginAccessBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val action = intent.action ?: return
        when (action) {
            Strings.ACTION_TRIGGER_REQUEST_ACCESS -> requestAccess(ctx, intent)
            Strings.ACTION_RECEIVE_ACCESS -> receiveAccess(ctx, intent)
            Strings.ACTION_REVOKE_ACCESS -> revokeAccess(ctx, intent)
            else -> {}
        }
    }

    private fun revokeAccess(ctx: Context, intent: Intent) {
        val senderPackage = intent.getStringExtra(Strings.EXTRA_SENDER)
        val accessToken = intent.getStringExtra(Strings.EXTRA_ACCESS_TOKEN)
        AccessManager.removeAccessToken(ctx, senderPackage, accessToken)
    }


    private fun receiveAccess(ctx: Context, intent: Intent) {
        val senderPackage = intent.getStringExtra(Strings.EXTRA_SENDER)
        val accessToken = intent.getStringExtra(Strings.EXTRA_ACCESS_TOKEN)
        AccessManager.storeAccessToken(ctx, senderPackage, accessToken, scopes)
    }

    private fun requestAccess(ctx: Context, intent: Intent) {
        val senderPackage = intent.getStringExtra(Strings.EXTRA_SENDER)
        val requestToken = intent.getStringExtra(Strings.EXTRA_REQUEST_TOKEN)
        val rpi = Intent(Strings.ACTION_REQUEST_ACCESS)
        rpi.setPackage(senderPackage)
        rpi.putExtra(Strings.EXTRA_SENDER, ctx.packageName)
        rpi.putExtra(Strings.EXTRA_REQUEST_TOKEN, requestToken)

        val token: String? = AccessManager.tryGetAccessToken(ctx, senderPackage, scopes)
        rpi.putExtra(Strings.EXTRA_ACCESS_TOKEN, token)

        rpi.putStringArrayListExtra(Strings.EXTRA_SCOPES, scopes)
        ctx.sendBroadcast(rpi)
    }

    private val scopes: ArrayList<String?> = ArrayList(
        listOf(
            Strings.SCOPE_QUERY_CREDENTIALS_FOR_OWN_PACKAGE,
        )
    )
}
