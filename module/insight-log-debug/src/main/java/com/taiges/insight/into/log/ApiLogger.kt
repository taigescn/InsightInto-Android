package com.taiges.insight.into.log

import com.google.gson.Gson
import com.taiges.insight.into.api.Request
import com.taiges.insight.into.common.log.ILog
import com.taiges.insight.into.common.log.Logger
import java.net.HttpURLConnection

/**
 * 开发、测试环境网络日志输出类
 */
open class ApiLogger : Logger {

    private val tag = "ApiLogger"

    override fun message(msg: String) {
    }

    override fun request(request: Request) {
        printRequestLog(request)
    }

    override fun response(
        request: Request,
        code: Int,
        response: String,
        conection: HttpURLConnection?,
        time: Long,
    ) {
        printResponseLog(request, code, response, conection, time)
    }

    /**
     * 打印请求日志
     */
    private fun printRequestLog(request: Request) {
        ILog.d(StringBuilder().apply {
            val methodName = request.httpMethod
            append("Request ")
            append(methodName)
            append(" ")
            append(request.url)
            append(" ")

            //构建请求头信息
            append("\n")
            val headersMap = request.header
            append("HEADERS:\n")
            for (entry in headersMap.entries) {
                append(entry.key)
                append(":")
                append(entry.value)
                append("\n")
            }

            val bodyLengthStr = if (request.hasBody()) {
                "(${request.getBodyLength()} byte body)"
            } else {
                ""
            }

            if (request.hasBody()) {
                append("BODY:\n")
                append(String(request.getBodyContent()))
                append("\n")
                append("OriginalBody:\n")
                val originalBody = Gson().toJson(request.body)
                append(originalBody)
                append("\n")
            }

            append("Request ")
            append(methodName)
            append(" ")
            append("END ")
            append(bodyLengthStr)


        }.toString(), tag)

    }

    /**
     * 打印请求响应日志
     */
    private fun printResponseLog(
        request: Request, code: Int, response: String, conection: HttpURLConnection?, time: Long,
    ) {
        ILog.d(StringBuilder().apply {
            val methodName = request.httpMethod
            append("\n")
            append("Response ")
            append(methodName)
            append(" ")
            append(request.url)
            append(" ($code)")

            val bodyLength = if (response.isNotEmpty()) {
                "($time ms,${response.toByteArray().size} byte body)"
            } else {
                ""
            }

            append("\n")
            val headersMap = parseResponseHeaders(conection)
            append("HEADERS:\n")
            for (entry in headersMap.entries) {
                append(entry.key)
                append(":")
                append(entry.value)
                append("\n")
            }

            if (response.isNotEmpty()) {
                append("BODY:\n")
                append(response)
                append("\n")
            }

            append("Response ")
            append(methodName)
            append(" ")
            append("END ")
            append(bodyLength)
            append("\n\n")
        }.toString(), tag)
    }

    /**
     * 解析响应结果请求头
     */
    private fun parseResponseHeaders(
        httpConection: HttpURLConnection?,
    ): Map<String, List<String>> {
        val hdeaders = mutableMapOf<String, List<String>>()
        httpConection?.headerFields?.let {
            for (mutableEntry in it) {
                mutableEntry.key?.let { key ->
                    hdeaders[key] = mutableEntry.value ?: listOf()
                }
            }
        }
        return hdeaders
    }
}