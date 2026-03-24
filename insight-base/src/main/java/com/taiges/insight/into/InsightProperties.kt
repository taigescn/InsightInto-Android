package com.taiges.insight.into

import android.content.Context
import android.text.TextUtils
import com.taiges.insight.into.bean.PageNativeData
import com.taiges.insight.into.common.SpPrefs
import com.taiges.insight.into.common.CommonUtil
import com.taiges.insight.into.common.CommonUtil.Companion.createInsightUUID
import com.taiges.insight.into.common.gson.GsonManager
import com.taiges.insight.into.common.log.ILog
import com.taiges.insight.into.common.tryBiz

/**
 * 公共对外属性类
 */
class InsightProperties(private val insightConfig: InsightConfig) {

    private val ctx: Context = insightConfig.context

    private val sessionId by lazy {
        createInsightUUID()
    }

    /**
     * 用户隐私政策协议
     */
    private var isPrivacyPolicy: Boolean? = null

    /**
     * 用户 Id
     */
    private var userId: String? = null

    /**
     * 用户实名 Id
     */
    private var uniqueId: String? = null

    /**
     * 用户手机号码
     */
    private var phoneNumber: String? = null

    /**
     * 登录会话Id
     */
    private var loginSessionId: String? = null

    /**
     * 会话级属性
     */
    private var ssProperties = mapOf<String, Any>()

    /**
     * 原生页面信息
     */
    private var pageNativeInfo = PageNativeData()

    /**
     * WebView(H5)页面映射信息，当前原生页面(实例Key)对应的H5页面信息
     *
     */
    private val pageWebInfoMap = mutableMapOf<String, Map<String, String>>()

    /**
     * 获取会话 Id
     */
    fun getSessionId() = sessionId

    /**
     * 设置隐私协议状态
     */
    @Synchronized
    internal fun setPrivacyPolicy(context: Context, isPrivacyPolicy: Boolean) {
        tryBiz {
            this.isPrivacyPolicy = isPrivacyPolicy
            ILog.d("setPrivacyPolicy:${isPrivacyPolicy}")
            SpPrefs.putBool(context, "PrivacyPolicy", isPrivacyPolicy)
        }
    }

    /**
     * 获取隐私协议状态
     */
    @Synchronized
    internal fun getPrivacyPolicy(context: Context): Boolean {
        return isPrivacyPolicy ?: SpPrefs.getBool(context, "PrivacyPolicy", false).let {
            isPrivacyPolicy = it
            it
        }
    }

    /**
     * 缓存用户 Id
     */
    @Synchronized
    fun setUserId(userId: String) {
        tryBiz {
            this.userId = userId
            ILog.d("setUserId:uid")
            SpPrefs.putAesStr(ctx, "UserId", userId)
            if (TextUtils.isEmpty(userId)) {
                //如果 userId 为空串则认为是退出登录，需要重置登录会话 Id
                resetLoginSessionId()
            }
        }
    }

    /**
     * 获取用户 Id
     */
    @Synchronized
    internal fun getUserId(): String {
        return userId ?: SpPrefs.getAesStr(ctx, "UserId").let {
            userId = it
            it
        }
    }

    /**
     * 缓存用户实名 Id
     */
    @Synchronized
    fun setUniqueId(uniqueId: String) {
        this.uniqueId = uniqueId
        tryBiz {
            ILog.d("setUniqueId:$uniqueId")
            SpPrefs.putAesStr(ctx, "UniqueId", uniqueId)
        }
    }

    /**
     * 获取用户实名 Id
     */
    @Synchronized
    internal fun getUniqueId(): String {
        return uniqueId ?: SpPrefs.getAesStr(ctx, "UniqueId").let {
            uniqueId = it
            it
        }
    }

    /**
     * 缓存用户手机号码
     */
    @Synchronized
    fun setPhoneNumber(phoneNumber: String) {
        this.phoneNumber = phoneNumber
        tryBiz {
            ILog.d("PhoneNumber:$phoneNumber")
            SpPrefs.putAesStr(ctx, "PhoneNumber", phoneNumber)
        }
    }

    /**
     * 获取用户手机号码
     */
    @Synchronized
    internal fun getPhoneNumber(): String {
        return phoneNumber ?: SpPrefs.getAesStr(ctx, "PhoneNumber").let {
            phoneNumber = it
            it
        }
    }

    private fun resetLoginSessionId(): String {
        return CommonUtil.createInsightUUID().also { uuid ->
            loginSessionId = uuid
            SpPrefs.putAesStr(ctx, "LoginSessionId", uuid)
        }
    }

    @Synchronized
    internal fun getLoginSessionId(): String {
        return loginSessionId ?: SpPrefs.getAesStr(ctx, "LoginSessionId").let {
            val tempId = if (TextUtils.isEmpty(it)) resetLoginSessionId() else it
            loginSessionId = tempId
            tempId
        }
    }


    /**
     * 移除会话级属性
     */
    @Synchronized
    fun removeSessionProperties(keys: List<String>) {
        val dbProperties = getSessionProperties()
        val newProperties = dbProperties.toMutableMap().also {
            for (key in keys) {
                it.remove(key)
            }
        }
        setSessionProperties(newProperties)
    }

    /**
     * 添加会话级属性
     */
    @Synchronized
    fun addSessionProperties(properties: Pair<String, Any>) {
        addSessionProperties(mapOf(properties))
    }

    /**
     * 添加会话级属性
     */
    @Synchronized
    fun addSessionProperties(properties: Map<String, Any>) {
        val dbProperties = getSessionProperties()
        val newProperties = dbProperties.toMutableMap().apply { putAll(properties) }
        setSessionProperties(newProperties)
    }

    /**
     * 缓存会话级属性
     */
    @Synchronized
    private fun setSessionProperties(properties: Map<String, Any>) {
        ssProperties = properties
        ILog.d("dbSessionProperties:${GsonManager.gson.toJson(properties)}")
    }

    /**
     * 获取会话级属性
     */
    @Synchronized
    internal fun getSessionProperties(): Map<String, Any> {
        return ssProperties
    }

    /**
     * 获取设备匿名Id(设备指纹)
     */
    fun getAnonymityId(): String {
        return insightConfig.getAnonymityId().id
    }

    /**
     * 获取 Adid
     */
    fun getAdid(): String {
        return CommonUtil.getAdidInfo(insightConfig.context).adid
    }

    /**
     * 缓存原生页面信息
     */
    fun setPageNativeData(pageNativeData: PageNativeData) {
        this.pageNativeInfo = pageNativeData
    }

    /**
     * 缓存指定页面 WebView 页面信息
     */
    fun setPageWebViewData(
        pageCurrentKey: String,
        pageWebUrl: String,
        pageWebPath: String,
        pageWebTitle: String,
    ) {
        val map = mapOf(
            "pageWebUrl" to pageWebUrl, "pageWebPath" to pageWebPath,
            "pageWebTitle" to pageWebTitle
        )
        pageWebInfoMap[pageCurrentKey] = map
    }

    /**
     * 移除指定页面 WebView 页面信息
     */
    fun removePageWebViewData(pageCurrentKey: String) {
        pageWebInfoMap.remove(pageCurrentKey)
    }

    internal fun getPageData(): Map<String, String> {
        val map = mutableMapOf(
            "pageWebUrl" to "",
            "pageWebPath" to "",
            "pageWebTitle" to ""
        )
        map.putAll(pageNativeInfo.getMapInfo())
        pageNativeInfo.pageCurrentKey?.takeIf { it.isNotEmpty() }?.also { pageCurrentKey ->
            pageWebInfoMap[pageCurrentKey]?.also {
                map.putAll(it)
            }
        }
        return map
    }

    /**
     * 获取业务 Api 需要透传的的 SDK 请求头参数，该透传时最好沟通约定增加前缀，避免与业务请求头字段冲突
     */
    fun getApiHeaders(): Map<String, String> {
        return mapOf(
            "client-id" to getAnonymityId(),
            "session-id" to getSessionId(),
            "sdk-version" to getSdkVersion(),
            "terminal-key" to insightConfig.terminalKey,//终端应用 Key (应用标识)，采集系统生成的key
            "platform" to CommonUtil.PLATFORM,//平台类型
        )
    }

    /**
     * 获取 SDK 版本号
     */
    fun getSdkVersion() = BuildConfig.SDK_VERSION

}