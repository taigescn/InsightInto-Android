package com.taiges.insight.into

import android.content.Context
import com.taiges.insight.into.bean.PermissionData
import com.taiges.insight.into.bean.ReaderParam
import com.taiges.insight.into.bean.ReaderResult
import com.taiges.insight.into.common.CommonUtil
import com.taiges.insight.into.common.log.Logger

/**
 * 采集数据提供者(外部提供数据采集实现)
 */
open class InsightIntoProvider {

    /**
     * 获取日志输出器
     */
    open fun getLogger(context: Context): Logger {
        return Logger.defaultLogger
    }

    /**
     * 获取设备环境数据采集服务
     */
    open fun getReaderService(insightConfig: InsightConfig): ReaderService {
        return object : ReaderService {
            override fun supportReaderItems() = listOf<String>()

            override fun getAnonymityId(context: Context) = CommonUtil.getAnonymityId(context)

            override fun exce(readerParam: ReaderParam) = ReaderResult(readerParam)

            override fun parsePermissionResult(
                requestCode: Int,
                permissions: Array<out String>,
                grantResults: IntArray
            ): PermissionData {
                return PermissionData.create(requestCode, permissions, grantResults)
            }
        }
    }

}