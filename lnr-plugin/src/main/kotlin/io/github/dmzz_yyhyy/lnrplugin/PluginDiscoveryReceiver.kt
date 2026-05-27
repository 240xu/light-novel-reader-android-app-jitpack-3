package io.github.dmzz_yyhyy.lnrplugin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Plugin discovery receiver.
 * LNR host sends a broadcast to discover installed plugins.
 * This receiver responds to the plugin discovery intent.
 */
class PluginDiscoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // Plugin discovery is handled by the KSP-generated code
    }
}
