package com.taiges.insight.into.common.log

import com.taiges.insight.into.api.Request
import java.net.HttpURLConnection

/**
 * 业务逻辑异常日志处理器
 */
interface Logger {

    companion object {
        /**
         * 日志处理器空实现
         */
        @JvmField
        val defaultLogger = object : Logger {
            override fun message(msg: String) {
            }

            override fun request(request: Request) {
            }

            override fun response(request: Request, code: Int, response: String, conection: HttpURLConnection?, time: Long) {
            }
        }
    }

    fun message(msg: String)

    fun request(request: Request)

    fun response(request: Request, code: Int, response: String, conection: HttpURLConnection?, time: Long)
}