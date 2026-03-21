package com.taiges.insight.into.app.common

import com.taiges.insight.into.app.App
import com.taiges.insight.into.app.kit.InsightIntoKit

/**
 * 应用账户管理类
 */
object AccountManager {

    private val context = App.getInstance()
    private val isLoginKey = "AppIsLogin"
    private val userIdSpKey = "AppUserId"
    private val uniqueIdSpKey = "AppUniqueId"
    private val privacyPolicyKey = "AppPrivacyPolicy"
    private val phoneNumberSpKey = "AppPhoneNumber"
    private val defaultUserId = "500123"
    private val defaultUniqueId = "5a198bba838f4c61005789ab8624fe9d"
    private val defaultPhoneNumber = "57512331"

    private var isPrivacyPolicy: Boolean? = null
    private var isLogin: Boolean? = null
    private var userId: String? = null
    private var uniqueId: String? = null
    private var phoneNumber: String? = null

    fun setPrivacyPolicy(isPrivacyPolicy: Boolean) {
        AccountManager.isPrivacyPolicy = isPrivacyPolicy
        AppSpUtil.putBoolean(context, privacyPolicyKey, isPrivacyPolicy)
    }

    fun isPrivacyPolicy(): Boolean {
        return isPrivacyPolicy ?: AppSpUtil.getBoolean(context, privacyPolicyKey, false).also {
            isPrivacyPolicy = it
        }
    }

    /**
     * 设置登录状态标识
     */
    fun setIsLogin(isLogin: Boolean) {
        AccountManager.isLogin = isLogin
        AppSpUtil.putBoolean(context, isLoginKey, isLogin)
    }

    fun isLogin(): Boolean {
        return isLogin ?: AppSpUtil.getBoolean(context, isLoginKey, false).also { cacheIsLogin ->
            isLogin = cacheIsLogin
        }
    }

    /**
     * 用户登录成功或退出登录后设置用户 Id
     */
    fun setUserId(userId: String) {
        AccountManager.userId = userId.ifEmpty { defaultUserId }
        //注意：userId 实际场景请加密存储
        AppSpUtil.putString(context, userIdSpKey, userId)
        InsightIntoKit.setUserId(userId)
    }

    fun getUserId(): String {
        return userId ?: AppSpUtil.getString(context, userIdSpKey).let { cacheUserId ->
            cacheUserId.ifEmpty { defaultUserId }.also { userId = it }
        }
    }

    fun setUniqueId(uniqueId: String) {
        AccountManager.uniqueId = uniqueId.ifEmpty { defaultUniqueId }
        //注意：uniqueId 实际场景请加密存储
        AppSpUtil.putString(context, uniqueIdSpKey, uniqueId)
        InsightIntoKit.setUniqueId(uniqueId)
    }

    fun getUniqueId(): String {
        return uniqueId ?: AppSpUtil.getString(context, uniqueIdSpKey).let { cacheUniqueId ->
            cacheUniqueId.ifEmpty { defaultUniqueId }.also { uniqueId = it }
        }
    }

    fun setPhoneNumber(phoneNumber: String) {
        AccountManager.phoneNumber = phoneNumber.ifEmpty { defaultPhoneNumber }
        //注意：phoneNumber 实际场景请加密存储
        AppSpUtil.putString(context, phoneNumberSpKey, phoneNumber)
        InsightIntoKit.setPhoneNumber(phoneNumber)
    }

    fun getPhoneNumber(): String {
        return phoneNumber ?: AppSpUtil.getString(context, phoneNumberSpKey)
            .let { cachePhoneNumber ->
                cachePhoneNumber.ifEmpty { defaultPhoneNumber }.also { phoneNumber = it }
            }
    }

}