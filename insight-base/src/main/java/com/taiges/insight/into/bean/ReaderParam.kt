package com.taiges.insight.into.bean

import com.taiges.insight.into.InsightInto
import com.taiges.insight.into.bean.api.PolicyConfig

/**
 * 采集参数
 * @param event 事件信息
 * @param localEventConfig 本地事件配置信息
 */
class ReaderParam(
    val event: InsightInto.Event,
    val localEventConfig: LocalEventConfig,
    val policyConfig: PolicyConfig
) {

    /**
     * 获取耗时采集事件采集管道 Key
     */
    fun getTimeConsumingCategory(): String {
        val category = localEventConfig.category
        for (collectConfigItem in localEventConfig.readerConfigItems) {
            if (collectConfigItem.isTimeConsuming == 1) {
                return "$category-${collectConfigItem.name}-TC"
            }
        }
        return category
    }
}


