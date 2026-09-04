package com.taiges.insight.into.app.kit

import android.app.Activity
import android.content.Context
import com.taiges.insight.into.InsightConfig
import com.taiges.insight.into.InsightInto
import com.taiges.insight.into.InsightIntoProvider
import com.taiges.insight.into.ReaderService
import com.taiges.insight.into.app.App
import com.taiges.insight.into.app.BuildConfig
import com.taiges.insight.into.app.common.AccountManager
import com.taiges.insight.into.app.common.getCurrentKey
import com.taiges.insight.into.bean.AdidData
import com.taiges.insight.into.bean.PageNativeData
import com.taiges.insight.into.common.CommonUtil
import com.taiges.insight.into.common.log.Logger
import com.taiges.insight.into.core.ReaderServiceImpl
import com.taiges.insight.into.log.ApiLogger

/**
 * InsightInto 核心封装类
 */
object InsightIntoKit {

    private val context = App.getInstance()

    private val sessionProperties = mutableListOf<Pair<String, Any>>().also {
        it.add("isLogin" to AccountManager.isLogin())
    }

    private val insightInto = InsightInto.Builder()
        .setContext(context)
        .setTerminalKey(BuildConfig.TERMINAL_KEY)
        .setBaseUrl(BuildConfig.API_BASE_URL)
        .setAppKey("insight-android")
        .setAppCode("InsightInto")
        .setChannelCode("210011")
        .setDebug(BuildConfig.DEBUG)
        .setInsightIntoProvider(object : InsightIntoProvider() {
            override fun getLogger(context: Context): Logger {
                return ApiLogger()
            }

            override fun getReaderService(insightConfig: InsightConfig): ReaderService {
                return ReaderServiceImpl(insightConfig)
            }
        })
        .build()

    private var pageNativeInfo = PageNativeData()

    init {
        //添加默认的会话级业务属性
        insightInto.properties.addSessionProperties(sessionProperties.toMap())
    }

    /**
     * 获取当前应用会话属性集合
     */
    fun getSessionProperties() = sessionProperties

    /**
     * 设置当前原生页面数据
     */
    fun setPageNativeData(activity: Activity) {
        val currentPageInfo = PageNativeData()
        val currentKey = activity.getCurrentKey()
        currentPageInfo.pageCurrentKey = currentKey
        currentPageInfo.pageCurrentName = activity::class.java.name
        currentPageInfo.pagePreviousKey = pageNativeInfo.pageCurrentKey
        currentPageInfo.pagePreviousName = pageNativeInfo.pageCurrentName
        currentPageInfo.pageIsRoot = "false"
        pageNativeInfo = currentPageInfo
        setPageWebViewData(
            currentKey, "https://www.insight-into.io", "/doc/guide", "InsightInto 文档"
        )
        setPageNativeData(currentPageInfo)
    }

    /**
     * 返回 sdk 版本号
     */
    fun getSdkVersion() = insightInto.properties.getSdkVersion()

    /**
     * 获取业务 Api 需要透传的的 SDK 请求头参数，该透传时最好沟通约定增加前缀，避免与业务请求头字段冲突
     */
    fun getApiHeaders() = insightInto.properties.getApiHeaders()

    /**
     * 隐私政策协议
     */
    fun setPrivacyPolicy(agree: Boolean) {
        insightInto.setPrivacyPolicy(agree)
    }

    /**
     * 设置登录 UserId
     * 1.登录成功后，设置用户 UserId
     * 2.退出登录后，清除用户 UserId，传入空串
     * 注意：如果有退出登录事件，触发时机需在该 Api 之前调用
     */
    fun setUserId(userId: String) {
        insightInto.properties.setUserId(userId)
        val isLogin = userId.isNotEmpty()
        val isLoginPair = "isLogin" to isLogin
        //根据传入的 UserId 判断当前是否为登录状态，并添加至会话级属性中
        insightInto.properties.addSessionProperties(isLoginPair)
        for (sessionProperty in sessionProperties) {
            if (sessionProperty.first == "isLogin") {
                sessionProperties.remove(sessionProperty)
                sessionProperties.add(isLoginPair)
                break
            }
        }
    }

    /**
     * 设置
     */
    fun setUniqueId(uniqueId: String) {
        insightInto.properties.setUniqueId(uniqueId)
    }

    /**
     * 设置用户手机号码
     */
    fun setPhoneNumber(phoneNumber: String) {
        insightInto.properties.setPhoneNumber(phoneNumber)
    }

    /**
     * 添加会话属性
     */
    fun addSessionProperties(properties: Map<String, Any>) {
        insightInto.properties.addSessionProperties(properties)
    }

    /**
     * 移除会话属性
     */
    fun removeSessionProperties(keys: List<String>) {
        insightInto.properties.removeSessionProperties(keys)
    }

    /**
     * 授权结果回调
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        insightInto.receivePermissionsResult(requestCode, permissions, grantResults)
    }

    /**
     * 采集事件
     * @param ecode 事件 Code 名称
     * @param eventProperties 事件业务数据
     */
    fun sendEvent(ecode: String, eventProperties: Map<String, Any>) {
        insightInto.sendEvent(ecode, eventProperties)
    }

    /**
     * 设置原生页面数据
     */
    fun setPageNativeData(pageNativeData: PageNativeData) {
        insightInto.properties.setPageNativeData(pageNativeData)
    }

    /**
     * 设置指定页面 WebView 页面数据
     */
    fun setPageWebViewData(
        pageCurrentKey: String,
        pageWebUrl: String,
        pageWebPath: String,
        pageWebTitle: String,
    ) {
        insightInto.properties.setPageWebViewData(
            pageCurrentKey, pageWebUrl, pageWebPath, pageWebTitle
        )
    }

    /**
     * 移除指定页面 WebView 页面数据
     */
    fun removePageWebViewData(pageCurrentKey: String) {
        insightInto.properties.removePageWebViewData(pageCurrentKey)
    }

    /**
     * 获取 Google Adid
     */
    fun getAdid(): AdidData {
        return CommonUtil.getAdidInfo(context)
    }
}