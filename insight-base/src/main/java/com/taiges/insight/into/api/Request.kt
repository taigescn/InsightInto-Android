package com.taiges.insight.into.api

import android.util.Base64
import com.google.gson.annotations.Expose
import com.taiges.insight.into.common.encrypt.AesUtil
import com.taiges.insight.into.common.encrypt.RsaUtil
import com.taiges.insight.into.common.CommonUtil
import com.taiges.insight.into.common.gson.GsonManager
import com.taiges.insight.into.common.gson.ISkipField
import com.taiges.insight.into.common.toMd5
import com.taiges.insight.into.common.tryBiz
import com.taiges.insight.into.common.tryClose
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

/**
 * 请求实体
 */
class Request(
    val baseUrl: String,
    val path: String,
    val body: MutableMap<String, Any?>? = null,
    val httpMethod: String = "POST",
    val header: MutableMap<String, String> = mutableMapOf()
) {
    @ISkipField
    private var bodyLength = -1

    @ISkipField
    private var bodyContent = byteArrayOf()

    @delegate:ISkipField
    val url by lazy {
        val pathSeparator = "/"
        val baseSeparator = if (baseUrl.endsWith(pathSeparator)) "" else pathSeparator
        "$baseUrl${baseSeparator}$path"
    }

    companion object {
        //服务端 Api Path
        const val INTERFACE_CONFIG_BASIC = "collect/config"//查询配置 api
        const val INTERFACE_MESSAGE = "collect/event"//实时事件采集接口

        const val HEADER_TERMINAL_TIMESTAMP = "terminal-timestamp"
        const val HEADER_CATEGORY = "category"
        const val HEADER_PUBLIC_KEY = "public-key"
        const val HEADER_TIME_ZONE = "time-zone"
        const val HEADER_COMPRESS_METHOD = "compress-method"
        const val HEADER_SECRET_KEY = "secret-key"
        const val HEADER_USER_ID = "user-id"
        const val HEADER_UNIQUE_ID = "unique-id"
        const val HEADER_CONTENT_TYPE = "Content-Type"
    }

    init {
        //内容默认 json 类型
        body?.let {
            val json = GsonManager.gson.toJson(it)
            bodyContent = json.toByteArray()
            bodyLength = bodyContent.size
        }
        header[HEADER_CONTENT_TYPE] = "application/json; charset=UTF-8"
    }

    /**
     * 是否已压缩过
     */
    private fun isCompressed() = header.containsKey(HEADER_COMPRESS_METHOD)

    /**
     * 是否已加密过
     */
    private fun isEncrypted() =
        header.containsKey(HEADER_SECRET_KEY) && header.containsKey(HEADER_PUBLIC_KEY)

    /**
     * 是否为需要公共请求头的 Api
     */
    fun needIgnoreCommonHeaderApi() = path != INTERFACE_CONFIG_BASIC

    /**
     * 获取 Body 内容长度
     */
    fun getBodyLength() = bodyLength

    /**
     * 是否有消息体
     */
    fun hasBody() = bodyLength > 0

    /**
     * 获取 Body 内容
     */
    fun getBodyContent() = bodyContent

    /**
     * 执行 Body 内容压缩
     */
    fun compress() {
        //是否需要压缩数据 && 是否有 Body 数据 && 未压缩过数据
        if (hasBody() && !isCompressed()) {
            tryBiz("compress data exception!") {
                val out = ByteArrayOutputStream()
                val gzipOutputStream = GZIPOutputStream(out)
                gzipOutputStream.write(bodyContent)
                gzipOutputStream.tryClose()
                bodyContent = out.toByteArray()
                bodyLength = bodyContent.size
                header[HEADER_COMPRESS_METHOD] = "gz"
            }
        }
    }

    /**
     * 执行 Body 内容加密
     */
    fun encrypt(publicKey: String) {
        //是否需要加密 && 是否有 Body 数据 && 未加密过数据
        if (hasBody() && !isEncrypted()) {
            val aesPassword = CommonUtil.createRandomUUID()
            val aesBody = AesUtil.encrypt(bodyContent, aesPassword)
            val publicKeyMd5 = publicKey.toMd5()
            val publicKeyBytes = Base64.decode(publicKey, Base64.DEFAULT)
            //Rsa 公钥加密 AES 的随机密匙
            val rsaAesPassword = Base64.encodeToString(
                RsaUtil.encrypt(
                    aesPassword.toByteArray(), publicKeyBytes
                ), Base64.NO_WRAP
            )

            bodyContent = aesBody.toByteArray()
            bodyLength = bodyContent.size
            header[HEADER_SECRET_KEY] = rsaAesPassword
            header[HEADER_PUBLIC_KEY] = publicKeyMd5
        }
    }
}