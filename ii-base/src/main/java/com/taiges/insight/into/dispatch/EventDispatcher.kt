package com.taiges.insight.into.dispatch

import com.taiges.insight.into.InsightConfig
import com.taiges.insight.into.InsightInto
import com.taiges.insight.into.bean.LocalEventConfig
import com.taiges.insight.into.bean.ReaderResult
import com.taiges.insight.into.bean.ReaderParam
import com.taiges.insight.into.api.ApiService
import com.taiges.insight.into.common.ChannelBus
import com.taiges.insight.into.common.log.ILog

/**
 * 事件分发类
 */
internal class EventDispatcher(private val insightConfig: InsightConfig) {

    private val dataReaderChannelBusMap = mutableMapOf<String, ChannelBus<ReaderParam>>()
    private val uploadEventChannelBus = ChannelBus<ReaderResult>() //数据上报管道
    private val apiService = ApiService(insightConfig)

    init {
        //接收事件/数据上报
        uploadEventChannelBus.receive {
//            if (insightConfig.debug) {
//                ILog.d(
//                    "receive upload event:${it.readerParam.eventData.ecode},items:${
//                        it.readerBaseInfo.readerItems.joinToString()
//                    }"
//                )
//            }
            //上报实时事件数据
            apiService.uploadEvent(it)
        }
    }

    /**
     * 处理事件
     */
    fun handle(event: InsightInto.Event) {
        if (insightConfig.debug) {
            ILog.d("handle event:${event.ecode}")
        }
        //第一步：构建采集数据参数列表
        val collectParams = matchingEventReaderParams(event)
        //第二步：遍历采集数据对象，发送采集对应项数据消息
        for (collectParam in collectParams) {
            val channelBus = getChannelBus(collectParam)
            channelBus.send(collectParam)
        }
    }

    /**
     * 匹配事件配置集合
     */
    private fun matchingEventReaderParams(event: InsightInto.Event): List<ReaderParam> {
        val readerParams = mutableListOf<ReaderParam>()
        //匹配实时事件采集参数
        val terminalConfig = apiService.loadTerminalConfig()
        val configs = terminalConfig.configList
        for (config in configs) {
            if (event.ecode == config.ecode) {
                val readerParam = ReaderParam(event, config, terminalConfig.policyConfig)
                readerParams.add(readerParam)
            }
        }

        //如果未配置到实时采集参数，则创建默认的采集配置
        if (readerParams.isEmpty()) {
            val localEventConfig = LocalEventConfig.createDefaultEventConfig(event)
            val readerParam = ReaderParam(event, localEventConfig, terminalConfig.policyConfig)
            readerParams.add(readerParam)
        }

        if (insightConfig.debug) {
            ILog.d("match event size:${readerParams.size}")
        }

        return readerParams
    }

    /**
     * 根据事件分类获取采集通道
     */
    private fun getChannelBus(readerParam: ReaderParam): ChannelBus<ReaderParam> {
        val key = readerParam.getTimeConsumingCategory()
        if (insightConfig.debug) {
            ILog.d("ChannelBus category:$key")
        }
        return dataReaderChannelBusMap[key] ?: createChannelBus().also {
            dataReaderChannelBusMap[key] = it
        }
    }

    /**
     * 创建采集通道
     */
    private fun createChannelBus(): ChannelBus<ReaderParam> {
        return ChannelBus<ReaderParam>().also { bus ->
            bus.receive {
                val readerResult =
                    insightConfig.getReaderService()?.exce(it) ?: ReaderResult(readerParam = it)
                uploadEventChannelBus.send(readerResult)
            }
        }
    }

}