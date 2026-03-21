package com.taiges.insight.into.app.common

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences 工具类
 */
class AppSpUtil {

    companion object {
        private const val file_name = "App_Sp"
        private var prefs: SharedPreferences? = null

        private fun getPrefs(context: Context): SharedPreferences {
            if (prefs == null) {
                synchronized(this) {
                    if (prefs == null) {
                        prefs = context.getSharedPreferences(file_name, Context.MODE_PRIVATE)
                    }
                }
            }

            return prefs!!
        }

        fun putString(context: Context, key: String, value: String) {
            getPrefs(context).edit().putString(key, value).apply()
        }

        fun getString(context: Context, key: String): String {
            return getPrefs(context).getString(key, "") ?: ""
        }

        fun getString(context: Context, key: String, default: String? = null): String? {
            return getPrefs(context).getString(key, default)
        }

        fun putInt(context: Context, key: String, value: Int) {
            getPrefs(context).edit().putInt(key, value).apply()
        }

        fun getInt(context: Context, key: String, default: Int): Int {
            return getPrefs(context).getInt(key, default)
        }

        fun putLong(context: Context, key: String, value: Long) {
            getPrefs(context).edit().putLong(key, value).apply()
        }

        fun getLong(context: Context, key: String, default: Long): Long {
            return getPrefs(context).getLong(key, default)
        }

        fun putBoolean(context: Context, key: String, value: Boolean) {
            getPrefs(context).edit().putBoolean(key, value).apply()
        }

        fun getBoolean(context: Context, key: String, default: Boolean): Boolean {
            return getPrefs(context).getBoolean(key, default)
        }

        fun clear(context: Context) {
            getPrefs(context).edit().clear().apply()
        }

        fun remove(context: Context, key: String) {
            getPrefs(context).edit().remove(key).apply()
        }
    }

}