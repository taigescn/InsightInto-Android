package com.taiges.insight.into.bean

/**
 * 上报采集事件数据缓存
 */
class UploadEventData(
    val uploadId: String,
    val headers: MutableMap<String, String> = mutableMapOf(),
    val body: MutableMap<String, Any?>? = null
)