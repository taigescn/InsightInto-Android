package com.taiges.insight.into

import android.content.Context
import com.taiges.insight.into.bean.AnonymityId
import com.taiges.insight.into.common.log.Logger

/**
 * 单例采集数据提供者
 */
internal class SingletonInsightIntoProvider(private val provider: InsightIntoProvider) :
    InsightIntoProvider() {

    private var logger: Logger? = null
    private var anonymityId: AnonymityId? = null
    private var readerService: ReaderService? = null

    companion object {

        fun create(provider: InsightIntoProvider?): InsightIntoProvider {
            val finalReaderProvider = provider ?: InsightIntoProvider()
            return SingletonInsightIntoProvider(finalReaderProvider)
        }
    }

    /**
     * 获取日志输出器
     */
    override fun getLogger(context: Context): Logger {
        return logger ?: provider.getLogger(context).also {
            logger = it
        }
    }

    /**
     * 获取设备环境数据采集服务
     */
    override fun getReaderService(insightConfig: InsightConfig): ReaderService {
        return readerService ?: provider.getReaderService(insightConfig).also {
            readerService = it
        }
    }
}