package com.taiges.insight.into

import android.content.Context
import com.taiges.insight.into.bean.PermissionData
import com.taiges.insight.into.common.ChannelBus
import com.taiges.insight.into.common.log.ILog
import com.taiges.insight.into.common.CommonUtil
import com.taiges.insight.into.common.toInteger
import com.taiges.insight.into.common.tryBiz
import com.taiges.insight.into.dispatch.EventDispatcher

/**
 * 采集入口类
 */
class InsightInto private constructor(private val insightConfig: InsightConfig) {

    companion object {
        //采集数据常量标识
        const val DEF_UNKNOWN = "UNKNOWN" //未知文本
        const val DEF_INT = -999 //未知
        const val DEF_LONG = -999L //未知
        const val DEF_L = 0L //long 字段默认值
        const val DEF_F = 0.0f //float 字段默认值
    }

    /**
     * 对外属性设置类
     */
    val properties by lazy { insightConfig.properties }
    private val eventChannelBus = ChannelBus<Event>()
    private val eventList = mutableListOf<Event>()
    private val eventDispatcher by lazy { EventDispatcher(insightConfig) }

    /**
     * 执行初始化，仅加载必要类，在未同意隐私政策协议前，不会执行任何采集
     */
    init {
        tryBiz("SDK init exception!") {
            ILog.init(insightConfig)

            //如果已同意隐私政策，则直接开始工作
            eventChannelBus.receive {
                val agreePrivacyPolicy = insightConfig.privacyPolicy
                if (insightConfig.debug) {
                    ILog.d("Worker receive event:${it.ecode},agreePrivacyPolicy:$agreePrivacyPolicy")
                }

                if (agreePrivacyPolicy || it.ecode == "PRIVACY_POLICY") {
                    //已同意隐私政策协议或隐私政策协议事件，处理事件分发逻辑
                    //先处理事件
                    eventDispatcher.handle(it)
                    batchDispatchEvent()
                } else {
                    //未同意隐私政策协议
                    eventList.add(it)
                }
            }

            addChannelBus(Event(ecode = "LAUNCH"))
        }
    }

    /**
     * 设置隐私协议，请只在用户同意或撤销隐私政策协议后调用此方法，其它场景不调用
     * @param isPrivacyPolicy 是否同意隐私政策协议
     */
    @Synchronized
    fun setPrivacyPolicy(isPrivacyPolicy: Boolean) {
        tryBiz("setPrivacyPolicy exception!") {
            if (insightConfig.debug) {
                ILog.d("setPrivacyPolicy isAgree:$isPrivacyPolicy,config.privacyPolicy:${insightConfig.privacyPolicy}")
            }
            insightConfig.privacyPolicy = isPrivacyPolicy

            if (isPrivacyPolicy) {
                batchDispatchEvent()
            }

            val cachePrivacyPolicy = properties.getPrivacyPolicy(insightConfig.context)
            val isChange = isPrivacyPolicy != cachePrivacyPolicy
            //检测隐私政策协议是否发生变化
            if (isChange) {
                properties.setPrivacyPolicy(insightConfig.context, isPrivacyPolicy)
                //隐私政策协议状态发生变化，发送隐私政策协议事件
                addChannelBus(
                    Event(
                        ecode = "PRIVACY_POLICY",
                        properties = mapOf("isAgree" to isPrivacyPolicy.toInteger())
                    )
                )
            }
        }
    }

    /**
     * 接收授权结果回调
     */
    fun receivePermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray,
    ) {
        tryBiz("receivePermissionsResult exception!") {
            val permissionData =
                PermissionData.create(insightConfig.context, requestCode, permissions, grantResults)
            val event =
                Event(ecode = "PERMISSION", properties = permissionData.eventProperties)
            event.permissionData = permissionData
            addChannelBus(event)
        }
    }

    /**
     * 触发原生事件
     * @param ecode 事件 Code 名称
     * @param properties 事件业务数据
     */
    fun sendEvent(ecode: String, properties: Map<String, Any> = mapOf()) {
        tryBiz("sendEvent exception!") {
            val event = Event(ecode = ecode, type = "TRACK", properties = properties)
            addChannelBus(event)
        }
    }

    /**
     * 获取 SDK 版本号
     */
    fun getSdkVersion() = BuildConfig.SDK_VERSION

    @Synchronized
    private fun batchDispatchEvent() {
        val hasCacheEventData = eventList.isNotEmpty()
        if (insightConfig.debug) {
            ILog.d("batchDispatchEvent hasCacheEventData:$hasCacheEventData")
        }
        if (hasCacheEventData) {
            val tempEventList = mutableListOf<Event>()
            tempEventList.addAll(eventList)
            eventList.clear()

            for (event in tempEventList) {
                //添加事件至队列中
                eventChannelBus.send(event)
            }
        }
    }

    /**
     * 将事件添加至队列中
     */
    private fun addChannelBus(event: Event) {
        //提前设置 UserId 至事件中，尽量保证事件 UserId 与实际场景匹配
        event.userId = insightConfig.properties.getUserId()
        event.uniqueId = insightConfig.properties.getUniqueId()
        event.phoneNumber = insightConfig.properties.getPhoneNumber()
        //事件触发时获取当前页面标识
        event.pageDataMap = insightConfig.properties.getPageData()
        event.loginSessionId = insightConfig.properties.getLoginSessionId()
        //添加事件至队列中
        eventChannelBus.send(event)
    }

    class Event(
        val ecode: String,
        val type: String = "INSIDE",
        val properties: Map<String, Any> = mapOf(),
        val msgId: String = CommonUtil.createInsightUUID(),
        val time: Long = System.currentTimeMillis(),
        val sdkSource: String = CommonUtil.PLATFORM,
        var userId: String = "",
        var uniqueId: String = "",
        var phoneNumber: String = "",
        var loginSessionId: String = "",
        var pageDataMap: Map<String, String> = mapOf()
    ) {
        var permissionData: PermissionData? = null

        fun isBridge() = sdkSource != CommonUtil.PLATFORM
    }

    class Builder {
        private var context: Context? = null
        private var terminalKey: String = ""
        private var appKey: String = ""
        private var appCode: String = ""
        private var channelCode: String = ""
        private var baseUrl: String = ""
        private var debug: Boolean = false //默认为非 Debug 环境
        private var provider: InsightIntoProvider? = null

        /**
         * 设置上下文
         */
        fun setContext(context: Context) = apply { this.context = context.applicationContext }

        /**
         * 设置 TerminalKey
         */
        fun setTerminalKey(terminalKey: String) = apply { this.terminalKey = terminalKey }

        /**
         * 设置 AppKey
         */
        fun setAppKey(appKey: String) = apply { this.appKey = appKey }

        /**
         * 设置 AppCode
         */
        fun setAppCode(appCode: String) = apply { this.appCode = appCode }

        /**
         * 设置 ChannelCode
         */
        fun setChannelCode(channelCode: String) = apply { this.channelCode = channelCode }

        /**
         * 设置服务端 Url
         */
        fun setBaseUrl(baseUrl: String) = apply { this.baseUrl = baseUrl }

        /**
         * 设置是否为 Debug 环境
         */
        fun setDebug(debug: Boolean) = apply { this.debug = debug }

        /**
         * 设置采集数据提供者
         */
        fun setInsightIntoProvider(provider: InsightIntoProvider) =
            apply { this.provider = provider }

        /**
         * 构建 Insight 实例
         */
        fun build(): InsightInto {
            val finalContext = context ?: throw IllegalArgumentException("context required!")
            val finalProvider = SingletonInsightIntoProvider.create(provider)

            return InsightInto(
                InsightConfig(
                    context = finalContext,
                    terminalKey = terminalKey,
                    appKey = appKey,
                    appCode = appCode,
                    channelCode = channelCode,
                    baseUrl = baseUrl,
                    debug = debug,
                    privacyPolicy = false,
                    provider = finalProvider
                )
            )
        }
    }
}