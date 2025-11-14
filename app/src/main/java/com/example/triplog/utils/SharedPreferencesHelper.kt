package com.example.triplog.utils

import android.content.Context
import android.content.SharedPreferences

object SharedPreferencesHelper {
    private const val PREFS_NAME = "trip_log_prefs"
    private const val KEY_LOGGED_IN_USER = "logged_in_user_email"

    fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveLoggedInUser(context: Context, email: String) {
        getSharedPreferences(context).edit().putString(KEY_LOGGED_IN_USER, email).apply()
    }

    fun getLoggedInUser(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_LOGGED_IN_USER, null)
    }

    fun clearLoggedInUser(context: Context) {
        getSharedPreferences(context).edit().remove(KEY_LOGGED_IN_USER).apply()
    }
}

