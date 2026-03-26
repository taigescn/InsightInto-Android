package com.taiges.insight.into.bean

import com.taiges.insight.into.bean.api.PolicyConfig

/**
 * 本地配置(服务端配置映射，展开采集项)
 */
internal class LocalConfig(
    val configList: List<LocalEventConfig> = listOf(),
    val policyConfig: PolicyConfig = PolicyConfig()
)