package com.taiges.insight.into.common.log

import android.util.Log
import com.taiges.insight.into.InsightConfig
import com.taiges.insight.into.common.tryI

/**
 * 日志工具类
 */
class ILog private constructor() {

    companion object {

        private const val TAG = "InsightLog"
        private var isShowLog = false //是否开启日志，默认关闭日志，测试环境会开启日志
        private var logger: Logger? = null

        /**
         * 日志初始化
         */
        fun init(insightConfig: InsightConfig) {
            // Debug 环境开启日志
            isShowLog = insightConfig.debug
            logger = insightConfig.getLogger()
        }

        /**
         * debug 日志
         */
        fun d(msg: String, tag: String = "${TAG}Log") {
            if (isShowLog) {
                Log.d(tag, msg)
                logger?.message("$TAG: $msg")
            }
        }

        /**
         * 异常日志
         */
        fun e(msg: String, t: Throwable? = null, tag: String = "${TAG}Error") {
            tryI {
                Log.e(tag, msg, t)
                t?.printStackTrace()
                val exceptionMsg = t?.let { ",error:\n${Log.getStackTraceString(t)}" } ?: ""
                val message = "$msg$exceptionMsg"
                logger?.message("$TAG: $message")
            }
        }

        /**
         * 业务逻辑异常
         */
        fun buns(msg: String, t: Throwable? = null, tag: String = TAG) {
            e(msg, t, tag)
        }
    }
}