package com.taiges.insight.into.common

import android.content.Context
import android.content.SharedPreferences
import com.taiges.insight.into.common.encrypt.AesUtil

/**
 * SharedPreferences 工具类
 */
class SpPrefs {

    companion object {
        private var prefs: SharedPreferences? = null

        private fun getPrefs(context: Context): SharedPreferences {
            if (prefs == null) {
                synchronized(this) {
                    if (prefs == null) {
                        prefs = context.getSharedPreferences("InsightIntoSpPrefs", Context.MODE_PRIVATE)
                    }
                }
            }

            return prefs!!
        }

        fun putAesStr(context: Context, key: String, value: String) {
            val aesValue = AesUtil.encryptStr(value, CommonUtil.akx)
            getPrefs(context).edit().putString(key, aesValue).apply()
        }

        fun getAesStr(context: Context, key: String): String {
            val aesValue = getPrefs(context).getString(key, null)
            return aesValue?.let { AesUtil.decryptStr(it, CommonUtil.akx) } ?: ""
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

        fun putBool(context: Context, key: String, value: Boolean) {
            getPrefs(context).edit().putBoolean(key, value).apply()
        }

        fun getBool(context: Context, key: String, default: Boolean): Boolean {
            return getPrefs(context).getBoolean(key, default)
        }
    }

}