package com.taiges.insight.into.bean.api

/**
 * 服务端 Response data 配置数据
 * @param eventConfigs 服务端事件配置列表
 * @param readerConfigs 服务端采集项配置列表
 * @param policyConfig 服务端策略配置
 */
internal class ServerData(
    val eventConfigs: List<EventConfig> = listOf(),
    val readerConfigs: List<ReaderConfig> = listOf(),
    val policyConfig: PolicyConfig = PolicyConfig(),
)