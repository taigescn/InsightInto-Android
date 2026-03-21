package com.taiges.insight.into.bean

import com.taiges.insight.into.InsightInto
import com.taiges.insight.into.bean.api.ReaderConfig
import com.taiges.insight.into.bean.api.EventConfig

/**
 * 本地事件配置(服务端配置映射，展开采集项)
 */
class LocalEventConfig(
    val ecode: String,
    val category: String,
    val readerConfigItems: List<ReaderConfig>,
) {

    companion object {

        //事件分类，Event：事件类
        const val CATEGORY_EVENT = "Event"

        /**
         * 创建默认事件配置
         */
        fun createDefaultEventConfig(event: InsightInto.Event): LocalEventConfig {
            return LocalEventConfig(
                event.ecode, CATEGORY_EVENT, listOf()
            )
        }

        /**
         * 展开本地事件配置
         * 1. 展开事件配置采集项
         * 2. 将事件配置展开为事件类配置和数据类配置
         */
        fun mapping(
            eventConfig: EventConfig, readerConfigItems: List<ReaderConfig>,
            supportReaderItems: List<String>
        ): List<LocalEventConfig> {
            val eventItems = mutableListOf<ReaderConfig>()
            val dataItems = mutableListOf<ReaderConfig>()
            val idArrs = eventConfig.items?.split(",") ?: listOf()
            for (id in idArrs) {
                for (item in readerConfigItems) {
                    if (id == item.itemId.toString() && supportReaderItems.contains(item.name)) {
                        if (item.isFollowEvent == 1) {
                            eventItems.add(item)
                        } else {
                            dataItems.add(item)
                        }
                    }
                }
            }

            val ecode = eventConfig.ecode
            val localEventConfigs = mutableListOf<LocalEventConfig>()
            //创建事件类配置
            localEventConfigs.add(
                LocalEventConfig(
                    ecode,
                    CATEGORY_EVENT,
                    eventItems
                )
            )

            if (dataItems.isNotEmpty()) {
                //创建数据类配置，每个数据采集项单独依赖一个事件上报
                for (dataItem in dataItems) {
                    localEventConfigs.add(
                        LocalEventConfig(
                            ecode,
                            dataItem.name,
                            mutableListOf(dataItem)
                        )
                    )
                }
            }

            return localEventConfigs
        }
    }
}