package com.taiges.insight.into.app.common

import android.util.Log

/**
 * 日志类
 */
class MLog private constructor() {

    companion object {

        private const val TAG = "DemoInsightInto"

        fun d(msg: String) {
            Log.d(TAG, msg)
        }

        fun i(msg: String, tag: String = TAG) {
            Log.i(tag, msg)
        }

        fun e(msg: String, throwable: Throwable? = null) {
            Log.e(TAG, msg, throwable)
        }
    }
}