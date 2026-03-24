package com.taiges.insight.into.api

/**
 * 请求实体
 */
class Request(
    val apiUrl: String,
    val header: MutableMap<String, String>,
    val bodyByteArray: ByteArray?,
    val bodyLength: Long,
    val originalBody:String?,
    val httpMethod: String = "POST"
) {

    /**
     * 是否有消息体
     */
    fun hasBody() = bodyLength > 0

    /**
     * 获取 Body 内容
     */
    fun getBodyContent() = bodyByteArray ?: byteArrayOf()
}