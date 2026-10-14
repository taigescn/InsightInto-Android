package com.taiges.insight.into.api

import android.annotation.SuppressLint
import android.os.Build
import android.os.SystemClock
import android.util.AndroidRuntimeException
import android.util.Base64
import com.google.gson.reflect.TypeToken
import com.taiges.insight.into.BuildConfig
import com.taiges.insight.into.InsightConfig
import com.taiges.insight.into.InsightInto
import com.taiges.insight.into.InsightInto.Companion.DEF_L
import com.taiges.insight.into.bean.ReaderResult
import com.taiges.insight.into.bean.LocalConfig
import com.taiges.insight.into.bean.LocalEventConfig
import com.taiges.insight.into.bean.UploadEventData
import com.taiges.insight.into.bean.api.BaseResult
import com.taiges.insight.into.bean.api.ServerData
import com.taiges.insight.into.common.SpPrefs
import com.taiges.insight.into.common.CommonUtil
import com.taiges.insight.into.common.GsonManager
import com.taiges.insight.into.common.db.DbOpenHelper
import com.taiges.insight.into.common.encrypt.AesUtil
import com.taiges.insight.into.common.encrypt.RsaUtil
import com.taiges.insight.into.common.fromJsonObject
import com.taiges.insight.into.common.log.ILog
import com.taiges.insight.into.common.saveJsonObject
import com.taiges.insight.into.common.toInteger
import com.taiges.insight.into.common.toSha256
import com.taiges.insight.into.common.formatTime
import com.taiges.insight.into.common.toMd5
import com.taiges.insight.into.common.tryBiz
import com.taiges.insight.into.common.tryClose
import com.taiges.insight.into.common.tryDisconnect
import com.taiges.insight.into.common.tryI
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.TimeZone
import java.util.Timer
import java.util.concurrent.Executors
import java.util.zip.GZIPOutputStream
import javax.net.ssl.*
import kotlin.concurrent.fixedRateTimer

/**
 * 网络客户端，使用 HttpURLConnection
 */
internal class ApiService(private val insightConfig: InsightConfig) {

    companion object {
        //服务端 Api Path
        const val INTERFACE_CONFIG_BASIC = "collect/config"//查询配置 api
        const val INTERFACE_EVENT = "collect/event"//实时事件采集接口

        const val HEADER_APP_KEY = "app-key"
        const val HEADER_APP_CODE = "app-code"
        const val HEADER_TERMINAL_KEY = "terminal-key"
        const val HEADER_PLATFORM = "platform"
        const val HEADER_SDK_VERSION = "sdk-version"
        const val HEADER_PACKAGE_NAME = "package-name"
        const val HEADER_CONTENT_TYPE = "Content-Type"
        const val HEADER_CALL_ID = "call-id"
        const val HEADER_TERMINAL_TIMESTAMP = "terminal-timestamp"
        const val HEADER_CATEGORY = "category"
        const val HEADER_PUBLIC_KEY = "public-key"
        const val HEADER_TIME_ZONE = "time-zone"
        const val HEADER_COMPRESS_METHOD = "compress-method"
        const val HEADER_SECRET_KEY = "secret-key"
        const val HEADER_USER_ID = "user-id"
        const val HEADER_UNIQUE_ID = "unique-id"
    }

    private val context = insightConfig.context
    private val keyServerData = "KEY_SERVER_DATA"
    private val keyServerDataCacheTime = "KEY_SERVER_DATA_CACHE_TIME"

    /**
     * 本地配置，用于事件采集项匹配
     */
    private var localConfig: LocalConfig? = null

    /**
     * 重试查询配置间隔时间
     */
    private var retryQueryConfigIntervalTime = 3000L

    /**
     * 最大重试查询配置间隔时间
     */
    private val maxRetryQueryConfigIntervalTime = 30000L

    /**
     * 上报数据尝试次数
     */
    private val uploadTryCount = 2

    /**
     * 公共请求头验签字段
     */
    private val signTargetKeys = listOf(
        HEADER_APP_KEY,
        HEADER_APP_CODE,
        HEADER_TERMINAL_KEY,
        HEADER_PLATFORM,
        HEADER_SDK_VERSION,
        HEADER_PACKAGE_NAME,
        HEADER_CALL_ID,
        HEADER_COMPRESS_METHOD,
        HEADER_SECRET_KEY,
        HEADER_PUBLIC_KEY,
        HEADER_CATEGORY,
        HEADER_USER_ID,
        HEADER_UNIQUE_ID,
        HEADER_TERMINAL_TIMESTAMP
    )

    private val commonHeaders by lazy {
        mutableMapOf(
            HEADER_APP_KEY to insightConfig.appKey,//业务终端编码
            HEADER_APP_CODE to insightConfig.appCode,//业务品牌/主体
            HEADER_TERMINAL_KEY to insightConfig.terminalKey,//终端应用 Key (应用标识)，采集系统生成的key
            HEADER_PLATFORM to CommonUtil.PLATFORM,//平台类型
            HEADER_SDK_VERSION to BuildConfig.SDK_VERSION,//SDK 版本号
            HEADER_CALL_ID to CommonUtil.createInsightUUID(),//请求唯一 id
            HEADER_PACKAGE_NAME to context.packageName,//应用包名
        )
    }

    private val singleThreadExecutor = Executors.newSingleThreadExecutor()
    private val supplementCacheDataDelay = 15000L //补报数据间隔 15 秒
    private var supplementCacheDataTimer: Timer? = null
    private val eventUploadList = mutableListOf<String>()
    private var elapsedRealtime = DEF_L
    private var serverTimestamp = DEF_L

    /**
     * 获取
     */
    fun loadTerminalConfig(): LocalConfig {
        return localConfig ?: queryTerminalConfig()
    }

    /**
     * 加载本地配置信息
     */
    @Synchronized
    private fun queryTerminalConfig(): LocalConfig {
        val serverData = getServerDataCache() ?: loadServerData()
        return parseServerData(serverData).also {
            localConfig = it

            if (supplementCacheDataTimer == null) {
                supplementCacheDataTimer = fixedRateTimer(
                    name = "SupplementCacheDataTimer",
                    initialDelay = supplementCacheDataDelay,
                    period = supplementCacheDataDelay
                ) {
                    tryI {
                        ILog.d("supplementCacheDataTimer exce time:${System.currentTimeMillis()}")
                        supplementEventRequest()
                    }
                }
            }
            //打印当前本地配置
            if (insightConfig.debug) {
                ILog.d(GsonManager.gson.toJson(it), "LocalConfig")
            }
        }
    }

    private fun loadServerData(): ServerData {
        var serverData: ServerData? = null
        while (serverData == null) {
            serverData = queryServerData()
            if (serverData == null) {
                try {
                    Thread.sleep(retryQueryConfigIntervalTime)
                } catch (_: InterruptedException) {
                }
                if (retryQueryConfigIntervalTime < maxRetryQueryConfigIntervalTime) {
                    //每次重试获取配等待时间延长两秒，降低频繁获取配置次数
                    retryQueryConfigIntervalTime += 2000
                }
            }
        }
        return serverData
    }

    /**
     * 上报实时事件数据
     */
    fun uploadEvent(readerResult: ReaderResult) {
        val uploadTimestamp = System.currentTimeMillis()
        val body = mutableMapOf<String, Any?>()
        val event = readerResult.readerParam.event
//        collectMode  String  是  采集模式，Code:代码埋点方式,Config:配置方式
        body["collectMode"] = "Code"
//        ecode  String  是  事件 code
        body["ecode"] = event.ecode
//        msgId  String  是  事件消息 Id，用于标识事件的唯一性
        body["msgId"] = event.msgId
//        eventTime  String  是  事件触发的时间(yyyy-MM-dd HH:mm:ss.SSS)
        body["eventTime"] = event.time.formatTime()
//        timestamp  String  是  事件触发的时间戳
        body["eventTimestamp"] = event.time
//        eventSource  String  是  事件触发源平台
        body["eventSource"] = event.sdkSource
//        eventType  String  是  事件类型,TRACK:埋点跟踪事件,INSIDE:内置事件
        body["eventType"] = event.type
//        uploadTime  String  是  采集数据上报时间(yyyy-MM-dd HH:mm:ss.SSS)
        body["uploadTime"] = uploadTimestamp.formatTime()
//        uploadTimestamp  String  是  采集时间戳
        body["uploadTimestamp"] = uploadTimestamp
        var clientCalibrationtTimestamp = DEF_L
        if (serverTimestamp != DEF_L && elapsedRealtime != DEF_L) {
            clientCalibrationtTimestamp =
                serverTimestamp - (elapsedRealtime - event.elapsedRealtime)
        }
//        clientCalibrationtTimestamp  String  是  终端校准时间戳
        body["clientCalibrationtTimestamp"] = clientCalibrationtTimestamp
//        collectorItems  Json 数组  否  采集项列表 Json 数组，具体参数见数据示例
        body["collectorItems"] = readerResult.readerBaseInfo.readerItems
//        userId  String  否  业务用户 Id，业务登录后必传
        body["userId"] = event.userId
//        uniqueId  String  否  业务用户实名 Id，实名后必传
        body["uniqueId"] = event.uniqueId
//        appKey  String  否   业务终端编码
        body["appKey"] = insightConfig.appKey
//        appCode  String  否   业务品牌/主体
        body["appCode"] = insightConfig.appCode
//        channelCode String  否   业务渠道编码
        body["channelCode"] = insightConfig.channelCode
//        phoneNumber  String  否  业务用户手机号码，业务登录后必传
        body["phoneNumber"] = event.phoneNumber
//        clientLoginSessionId  String  否  终端登录会话 id
        body["clientLoginSessionId"] = event.loginSessionId
//        eventProperties  Json 对象  否  事件属性，作用域仅为事件，具体参数见数据示例
        body["eventProperties"] = event.properties
//        sessionProperties  Json 对象  否  会话属性，作用域为整个会话，具体参数见数据示例
        body["sessionProperties"] = insightConfig.properties.getSessionProperties()
//        stayDurationTime  int  否  事件触发到事件上传的耗时
        body["stayDurationTime"] = uploadTimestamp - event.time
//        isBridge  int  是  是否为桥接事件
        body["isBridge"] = event.isBridge().toInteger()
//        appName  String  否  应用名称
        body["appName"] = CommonUtil.getAppName(context)
//        eventData  Json 数组  否  采集数据，数据结构为 Json 数组，具体参数
        body["eventData"] = readerResult.dataList
//        isSupplementCacheData int 是否为缓存补报数据，0：否，1：是
        body["isSupplementCacheData"] = 0
        //设置当前页面标识
        body.putAll(event.pageDataMap)

        val headers = mutableMapOf<String, String>()
        headers[HEADER_USER_ID] = event.userId
        headers[HEADER_UNIQUE_ID] = event.uniqueId
        val category = readerResult.readerParam.localEventConfig.category
        headers[HEADER_CATEGORY] = category
        headers[HEADER_TERMINAL_TIMESTAMP] = uploadTimestamp.toString()

        if (insightConfig.debug) {
            ILog.d("Start upload event:${event.ecode}")
        }

        val uploadId = "$category-${event.msgId}"
        eventUploadList.add(uploadId)

        val uploadEventData = buildUploadEventData(
            uploadId = uploadId, body = body, headers = headers
        )

        singleThreadExecutor.submit {
            tryI {
                ILog.d("Exce saveEventRequest uploadId:${uploadId}")
                DbOpenHelper.saveEventRequest(context, uploadEventData)
            }
        }

        val baseResult = try {
            uploadEventData.call<Any?>()
        } catch (e: Throwable) {
            BaseResult.createError<Any?>("uploadEvent exception:${e.message}")
        }

        if (insightConfig.debug) {
            ILog.d(
                "End upload event:${event.ecode},success:${baseResult.isSuccess()}"
            )
        }

        if (baseResult.isSuccess()) {
            readerResult.callReaderHandleBlocks()
            singleThreadExecutor.submit {
                tryI {
                    ILog.d("Exce deleteEventRequest uploadId:$uploadId")
                    DbOpenHelper.deleteEventRequest(context, uploadId)
                    eventUploadList.remove(uploadId)
                }
            }
        } else {
            eventUploadList.remove(uploadId)
        }
    }

    /**
     * 解析服务端配置
     * @param serverData 服务端配置
     */
    private fun parseServerData(serverData: ServerData): LocalConfig {
        //1. 将服务端配置的事件与采集项展开
        val localEventConfigs = mutableListOf<LocalEventConfig>()
        val supportReaderItems = insightConfig.getReaderService().supportReaderItems()
        for (eventConfig in serverData.eventConfigs) {
            val configs =
                LocalEventConfig.mapping(eventConfig, serverData.readerConfigs, supportReaderItems)
            localEventConfigs.addAll(configs)
        }

        //2. 创建本地配置实例
        return LocalConfig(localEventConfigs, serverData.policyConfig)
    }

    /**
     * 查询服务端配置
     */
    private fun queryServerData(): ServerData? {
        tryI("queryServerData exception!") {

            val request = buildRequest(INTERFACE_CONFIG_BASIC, commonHeaders)
            val configBaseResult = exceRequest<ServerData>(request)
            if (configBaseResult.isSuccess()) {
                val serverData = configBaseResult.data!!
                saveServerDataCache(serverData)

                //记录配置缓存时间
                SpPrefs.putLong(
                    context, keyServerDataCacheTime, System.currentTimeMillis()
                )
                ILog.d("queryServerData success!")
                return serverData
            } else {
                ILog.e(
                    "The query server configuration failed! configCode:${configBaseResult.code}, configMessage:${configBaseResult.message}"
                )

                if (configBaseResult.isCheckFail()) {
                    retryQueryConfigIntervalTime = maxRetryQueryConfigIntervalTime
                }
            }
        }
        return null
    }

    /**
     * 检查服务端的配置是否无效，30 分钟内有效
     */
    private fun checkServerDataVaild(): Boolean {
        val currentTime = System.currentTimeMillis()
        val serverDataCacheTime = SpPrefs.getLong(context, keyServerDataCacheTime, 0)
        return currentTime - serverDataCacheTime < 1800000
    }

    /**
     * 缓存服务端配置信息
     */
    private fun saveServerDataCache(serverData: ServerData) {
        saveJsonObject(context, keyServerData, serverData)
    }

    /**
     * 获取服务端配置信息缓存
     */
    private fun getServerDataCache(): ServerData? {
        if (checkServerDataVaild()) {
            return fromJsonObject<ServerData>(context, keyServerData)
        }
        return null
    }

    /**
     * 补报事件请求
     */
    @Synchronized
    private fun supplementEventRequest() {
        val dataCacheValidTime = loadTerminalConfig().policyConfig.dataCacheValidTime
        val eventList = DbOpenHelper.queryAllEvent(context, dataCacheValidTime)
        ILog.d("exce supplementCacheData size:${eventList.size}")
        for (eventData in eventList) {
            tryI {
                val uploadId = eventData.uploadId
                if (!eventUploadList.contains(uploadId)) {
                    ILog.d("SupplementCacheData uploadId:${uploadId}")
                    eventData.body?.also {
                        it["isSupplementCacheData"] = 1
                    }
                    val baseResult = eventData.call<Any?>()
                    if (baseResult.isSuccess()) {
                        ILog.d("exce deleteEventRequest uploadId:${uploadId}")
                        DbOpenHelper.deleteEventRequest(context, uploadId)
                    } else {
                        ILog.d("SupplementCacheData failed uploadId:${uploadId}")
                    }
                }
            }
        }
    }

    private fun buildUploadEventData(
        uploadId: String,
        headers: MutableMap<String, String> = mutableMapOf(),
        body: MutableMap<String, Any?>? = null
    ): UploadEventData {

        //添加公共请求头
        headers.putAll(commonHeaders)

        val collectTimestamp =
            headers[HEADER_TERMINAL_TIMESTAMP] ?: System.currentTimeMillis().toString()
        headers[HEADER_TERMINAL_TIMESTAMP] = collectTimestamp

        //client-id  String  是  设备匿名标识符
        headers["client-id"] = insightConfig.getAnonymityId().id
        //app-version  String  否  应用版本号，Android/iOS 端会有值
        headers["app-version"] = CommonUtil.getAppVersion(context)
        //app-vcode  String  否  应用版本号 Code，Android/iOS 端会有值
        headers["app-vcode"] = CommonUtil.getAppVersionCode(context).toString()
        if (!headers.containsKey(HEADER_USER_ID)) {
            //user-id  String  否  用户 Id，已登录状态时有值，未登录时传入空串
            headers[HEADER_USER_ID] = insightConfig.properties.getUserId()
        }

        if (!headers.containsKey(HEADER_UNIQUE_ID)) {
            //unqiue-id  String  否  用户实名 Id，已实名状态时有值，未实名时传入空串
            headers[HEADER_UNIQUE_ID] = insightConfig.properties.getUniqueId()
        }
        //session-id  String  是  会话 id
        headers["session-id"] = insightConfig.properties.getSessionId()
        //time-zone  String  是  时区编码
        headers[HEADER_TIME_ZONE] = TimeZone.getDefault().id

        return UploadEventData(
            uploadId = uploadId,
            headers = headers,
            body = body,
        )
    }

    /**
     * 请求 Api
     */
    private fun buildRequest(
        apiPath: String,
        headers: MutableMap<String, String> = mutableMapOf(),
        body: MutableMap<String, Any?>? = null
    ): Request {
        val baseUrl = insightConfig.baseUrl
        val pathSeparator = "/"
        val baseSeparator = if (baseUrl.endsWith(pathSeparator)) "" else pathSeparator
        val url = "$baseUrl${baseSeparator}$apiPath"

        headers[HEADER_CONTENT_TYPE] = "application/json; charset=UTF-8" //json 请求类型
        headers[HEADER_CALL_ID] = CommonUtil.createInsightUUID()//请求唯一 id

        var bodyLength = -1L
        var bodyContent = byteArrayOf()
        body?.let {
            val policyConfig = loadTerminalConfig().policyConfig
            val json = GsonManager.gson.toJson(it)
            json.toByteArray().also { bodyBytes ->
                bodyContent = bodyBytes
                bodyLength = bodyBytes.size.toLong()
                if (policyConfig.isOpenCompress()) {
                    //执行数据压缩
                    if (bodyLength > 0) {
                        tryBiz("compress data exception!") {
                            val out = ByteArrayOutputStream()
                            val gzipOutputStream = GZIPOutputStream(out)
                            gzipOutputStream.write(bodyContent)
                            gzipOutputStream.tryClose()
                            val compressBytes = out.toByteArray()
                            bodyContent = compressBytes
                            bodyLength = compressBytes.size.toLong()
                            headers[HEADER_COMPRESS_METHOD] = "gz"
                        }
                    }
                }

                if (policyConfig.isOpenEncrypt()) {
                    //执行数据加密
                    val publicKey = policyConfig.publicKey
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
                    bodyLength = bodyContent.size.toLong()
                    headers[HEADER_SECRET_KEY] = rsaAesPassword
                    headers[HEADER_PUBLIC_KEY] = publicKeyMd5
                }
            }
        }

        //data-sign  String  是  Body数据验签，值为 Sha256 ，长度为 64 位
        if (bodyLength > 0) {
            val dataSignText = "requestBody=$bodyLength"
            headers["data-sign"] = dataSignText.toSha256()
        }

        val signHeaders = mutableMapOf<String, String>()
        for (targetKey in signTargetKeys) {
            headers[targetKey]?.let {
                signHeaders[targetKey] = it
            }
        }

        //data-sign  String  是  请求头验签,值为 Sha256 ，长度为 64 位
        headers["head-sign"] = CommonUtil.createSign(signHeaders)

        return Request(
            url = url,
            headers = headers,
            body = body,
            bodyLength = bodyLength,
            bodyContent = bodyContent
        )
    }

    private inline fun <reified T> UploadEventData.call(): BaseResult<T?> {
        val request = buildRequest(apiPath = INTERFACE_EVENT, headers = headers, body = body)
        return exceRequest(request)
    }

    /**
     * 创建 HttpURLConnection 连接并设置相关配置
     */
    private fun openConnection(request: Request): HttpURLConnection {
        val url = URL(request.url)

        val connection = url.openConnection() as HttpURLConnection
        val hasBody = request.hasBody()
        connection.connectTimeout = 120 * 1000//设置连接超时时间
        connection.readTimeout = 120 * 1000//设置读取超时时间
        connection.requestMethod = request.httpMethod //设置 Http 请求方式
        connection.doInput = true
        connection.doOutput = hasBody
        connection.useCaches = false //不允许缓存

        if (connection is HttpsURLConnection) {
            connection.hostnameVerifier = HostnameVerifier { _, _ -> true }
        }

        return connection
    }

    /**
     * 发起请求调用
     */
    @SuppressLint("ObsoleteSdkInt")
    private inline fun <reified T> exceRequest(request: Request): BaseResult<T> {
        var baseResult: BaseResult<T> = BaseResult.createError("UnknownError")
        val start = System.currentTimeMillis()
        for (i in 1..uploadTryCount) {
            var connection: HttpURLConnection? = null
            var ops: OutputStream? = null
            var inps: InputStream? = null
            var code = InsightInto.DEF_INT
            var response = ""
            var exception: Exception? = null
            try {

                if (insightConfig.debug) {
                    insightConfig.getLogger().request(request)
                }

                connection = openConnection(request)

                for (entry in request.headers.entries) {
                    connection.setRequestProperty(entry.key, entry.value)
                }

                if (request.hasBody()) {
                    val contentLength = request.bodyLength
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                        //Body 长度未知，采用默认分块大小传输
                        connection.setChunkedStreamingMode(0)
                    } else {
                        //设置 Body 已知长度
                        connection.setFixedLengthStreamingMode(contentLength)
                    }
                    ops = connection.outputStream.apply {
                        write(request.bodyContent)
                        flush()
                    }
                }

                code = connection.responseCode
                if (code in 200..299) {
                    inps = connection.inputStream
                    response = String(connection.inputStream.readBytes())
                } else {
                    throw AndroidRuntimeException("network error!")
                }

                //校准终端时间
                calibrationServerTime(connection)

                val type = object : TypeToken<BaseResult<T>>() {}.type
                baseResult = GsonManager.gson.fromJson(response, type)
                if (baseResult.isSuccess() || i >= uploadTryCount) {
                    //第一次成功或已经重试过则直接返回结果
                    return baseResult
                }
            } catch (e: Exception) {
                val msg = "The $i th time request: ${request.url} ($code) ${e.message ?: ""}"
                if (i < uploadTryCount) {
                    ILog.e(msg, e)
                } else {
                    throw AndroidRuntimeException(msg, e).also { exception = it }
                }
            } finally {
                if (insightConfig.debug) {
                    val time = System.currentTimeMillis() - start
                    exception?.let {
                        tryI {
                            val stringWriter = StringWriter()
                            it.printStackTrace(PrintWriter(stringWriter))
                            response = stringWriter.toString()
                        }
                    }
                    insightConfig.getLogger().response(request, code, response, connection, time)
                }

                inps?.tryClose()
                ops?.tryClose()
                connection?.tryDisconnect()
            }
            //尝试 2 秒再上报事件
            Thread.sleep(2000)
        }

        return baseResult
    }

    private fun calibrationServerTime(connection: HttpURLConnection?) {
        if (serverTimestamp == DEF_L || elapsedRealtime == DEF_L) {
            connection?.apply {
                headerFields?.also { headers ->
                    tryI {
                        headers["server-timestamp"]?.also {
                            serverTimestamp = it[0].toLong()
                            elapsedRealtime = SystemClock.elapsedRealtime()
                        }
                    }
                }
            }
        }
    }
}