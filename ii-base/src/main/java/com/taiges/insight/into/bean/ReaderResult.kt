package com.taiges.insight.into.bean

import com.taiges.insight.into.common.log.ILog

/**
 * 采集数据对象，包含事件源、本地事件配置、策略配置、采集数据、上传回调 Block
 */
class ReaderResult(
    val readerParam: ReaderParam,
    val readerBaseInfo: ReaderBaseInfo = ReaderBaseInfo(),
    val dataList: MutableList<Map<String, Any?>> = mutableListOf(),
    val readerHandleBlocks: MutableList<() -> Unit> = mutableListOf(),
) {

    /**
     * 上报采集数据成功，回调采集器内部处理
     */
    fun callReaderHandleBlocks() {
        for (block in readerHandleBlocks) {
            try {
                block.invoke()
            } catch (e: Exception) {
                ILog.e("callReaderHandleBlocks Exception!", e)
            }
        }
    }

    class ReaderBaseInfo(
        var readerTime: String = "",
        var readerTimestamp: Long = 0,
        val errors: MutableMap<String, String> = mutableMapOf(),
        val durationTimes: MutableMap<String, Long> = mutableMapOf(),
        val readerItems: MutableList<String> = mutableListOf(),
    )
}