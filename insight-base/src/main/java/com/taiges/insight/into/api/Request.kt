package com.taiges.insight.into.api

/**
 * 请求实体
 */
class Request(
    val url: String,
    val headers: MutableMap<String, String> = mutableMapOf(),
    val body: MutableMap<String, Any?>? = null,
    val httpMethod: String = "POST",
    val bodyLength: Long = -1,
    val bodyContent: ByteArray = byteArrayOf()
) {

    /**
     * 是否有消息体
     */
    fun hasBody() = bodyLength > 0
}