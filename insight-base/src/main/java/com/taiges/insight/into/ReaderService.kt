package com.taiges.insight.into

import android.content.Context
import com.taiges.insight.into.bean.AnonymityId
import com.taiges.insight.into.bean.ReaderResult
import com.taiges.insight.into.bean.ReaderParam

/**
 * 设备环境数据采集服务
 */
interface ReaderService {

    /**
     * 获取支持的采集项
     */
    fun supportReaderItems(): List<String>

    /**
     * 获取设备匿名标识符
     */
    fun getAnonymityId(context: Context): AnonymityId

    /**
     * 执行设备环境数据采集
     */
    fun exce(readerParam: ReaderParam): ReaderResult

}