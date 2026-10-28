package com.phone.racehome

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!Prefs.isArmed(context)) return
        // Bring launcher UI up fast after boot / unlock
        val launch = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MainActivity.EXTRA_FROM_BOOT, true)
        }
        context.startActivity(launch)
        KeepAliveService.start(context)
    }
}
