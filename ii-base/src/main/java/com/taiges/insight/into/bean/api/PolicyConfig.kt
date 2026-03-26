package com.taiges.insight.into.bean.api

import android.text.TextUtils

/**
 * 服务端策略配置
 * @param isEncrypt 是否加密，0：不加密，1：加密
 * @param isCompress 是否压缩，0：不压缩，1：压缩
 * @param publicKey 加密 RSA 的公钥
 * @param dataCacheValidTime 事件缓存数据有效时间，默认：10分钟
 */
class PolicyConfig(
    private val isEncrypt: Int = 0,
    private val isCompress: Int = 0,
    val publicKey: String = "",
    val dataCacheValidTime: Long = 10 * 60 * 1000,
) {
    /**
     * 是否开启加密
     */
    fun isOpenEncrypt() = isEncrypt == 1 && !TextUtils.isEmpty(publicKey)

    /**
     * 是否开启压缩
     */
    fun isOpenCompress() = isCompress == 1
}