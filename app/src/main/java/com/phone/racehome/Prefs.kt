package com.phone.racehome

import android.content.Context

object Prefs {
    private const val NAME = "race_home"
    private const val KEY_ARMED = "armed"

    fun isArmed(context: Context): Boolean =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ARMED, false)

    fun setArmed(context: Context, armed: Boolean) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ARMED, armed)
            .apply()
    }
}
