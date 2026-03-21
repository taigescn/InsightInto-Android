package com.taiges.insight.into.log

import com.taiges.insight.into.api.Request
import com.taiges.insight.into.common.log.Logger
import java.net.HttpURLConnection

/**
 * 生产环境网络日志输出类
 */
open class ApiLogger : Logger {

    override fun message(msg: String) {
    }

    override fun request(request: Request) {
    }

    override fun response(
        request: Request,
        code: Int,
        response: String,
        conection: HttpURLConnection?,
        time: Long,
    ) {
    }
}