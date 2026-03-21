package com.taiges.insight.into

import android.content.Context

/**
 * SDK 配置
 */
class InsightConfig(
    val context: Context,
    val terminalKey: String,
    val baseUrl: String,
    val appCode: String,
    val appKey: String,
    val channelCode: String,
    val debug: Boolean,
    val provider: InsightIntoProvider,
    var privacyPolicy: Boolean = false,
) {

    internal val properties = InsightProperties(this)

    init {
        privacyPolicy = properties.getPrivacyPolicy(context)
    }

    /**
     * 设备环境数据采集服务
     */
    internal fun getReaderService() = provider.getReaderService(this)

    /**
     * 获取日志输出器
     */
    internal fun getLogger() = provider.getLogger(context)

    /**
     * 获取设备匿名标识符
     */
    fun getAnonymityId() = getReaderService().getAnonymityId(context)
}