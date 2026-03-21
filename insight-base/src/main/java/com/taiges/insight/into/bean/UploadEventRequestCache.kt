package com.taiges.insight.into.bean

import com.taiges.insight.into.api.Request

/**
 * 上报采集事件数据缓存
 */
class UploadEventRequestCache(
    val uploadId: String,
    val request: Request
)