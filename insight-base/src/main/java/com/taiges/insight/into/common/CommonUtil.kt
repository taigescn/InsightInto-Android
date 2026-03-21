package com.taiges.insight.into.common

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.taiges.insight.into.InsightInto
import com.taiges.insight.into.bean.AdidData
import com.taiges.insight.into.bean.AnonymityId
import com.taiges.insight.into.common.log.ILog
import java.util.UUID

/**
 * 工具类
 */
class CommonUtil private constructor() {

    companion object {
        //平台类型标识
        private const val INSIGHT_MARK = "a1"

        //平台类型
        const val PLATFORM = "android"

        // AndroidId 黑名单列表
        val androidIdBlackList = mutableListOf(
            "android", "0000000000000000", "&",
            "00000000000000000000000", "9774d56d682e549c", "0123456789abcdef",
            "00000000-0000-0000-0000-000000000000"
        )

        val sessionId by lazy {
            createInsightUUID()
        }

        private var sdkFirstInitTime: Long = InsightInto.DEF_L
        private var androidId: String? = null
        private var deviceModel: String? = null
        private var appFirstLaunch = InsightInto.DEF_INT
        private var appName: String? = null
        private var appVersionName: String? = null
        private var appVersionCode: Long? = null
        private var appPackageInfo: PackageInfo? = null
        private var adidResult = AdidData("", InsightInto.DEF_INT, "")
        private var adidReadTime = 0L

        val akx = getAkxStr()

        /**
         * 获取 App 首次初始化 sdk 时间戳
         */
        @Synchronized
        fun getSdkFirstInitTime(context: Context): Long {
            if (sdkFirstInitTime == InsightInto.DEF_L) {
                tryI {
                    sdkFirstInitTime =
                        SpPrefs.getLong(context, "SdkFirstInitTime", InsightInto.DEF_L)
                    if (sdkFirstInitTime == InsightInto.DEF_L) {
                        val currentTime = System.currentTimeMillis()
                        SpPrefs.putLong(
                            context, "SdkFirstInitTime", currentTime
                        )
                        sdkFirstInitTime = currentTime
                    }
                }
            }
            return sdkFirstInitTime
        }

        /**
         * 获取可验证的 UUID
         * 追加唯一Id (sessionId、CallId、msgId)校验位
         */
        fun createInsightUUID(): String {
            val uuid = createRandomUUID()
            return appendCheckMark(uuid)
        }

        /**
         * 随机生成小写去横杠 UUID
         */
        fun createRandomUUID(): String {
            return UUID.randomUUID().toString().replace("-", "").lowercase()
        }

        /**
         * 添加校验印记
         */
        fun appendCheckMark(uuid: String): String {
            val flagUuid = "$uuid$INSIGHT_MARK"
            var result = 0
            for (i in flagUuid.indices) {
                result += (flagUuid[i].code xor i)
            }
            return flagUuid + Integer.toHexString(result % 16).lowercase()
        }

        /**
         * 获取 AndroidId
         */
        @Synchronized
        @SuppressLint("HardwareIds")
        fun getAndroidId(context: Context): String {
            return androidId ?: context.let {
                val androidIdStr = Settings.Secure.getString(
                    it.contentResolver, Settings.Secure.ANDROID_ID
                ) ?: ""
                if (!TextUtils.isEmpty(androidIdStr.trim())) {
                    androidId = androidIdStr
                }
                androidIdStr
            }
        }

        /**
         * 获取设备型号
         */
        fun getBuildModel(): String {
            if (deviceModel == null) {
                val deviceModelStr = Build.MODEL
                if (!TextUtils.isEmpty(deviceModelStr)) {
                    deviceModel = deviceModelStr
                }
            }
            return deviceModel ?: ""
        }

        /**
         * 获取App版本号
         */
        fun getAppVersion(context: Context): String {
            return appVersionName ?: context.let {
                var verName = InsightInto.DEF_UNKNOWN
                tryI {
                    val packageInfo = getAppPackageInfo(context)
                    verName = packageInfo.versionName
                }
                appVersionName = verName
                verName
            }
        }

        /**
         * 获取App版本Code
         */
        fun getAppVersionCode(context: Context): Long {
            return appVersionCode ?: context.let {
                var verCode = 0L
                tryI {
                    val packageInfo = getAppPackageInfo(context)
                    verCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode
                    } else {
                        packageInfo.versionCode.toLong()
                    }
                }
                appVersionCode = verCode
                verCode
            }
        }

        /**
         * 获取App名称
         */
        fun getAppName(context: Context): String {
            return appName ?: getAppPackageInfo(context).getAppName(context).also {
                appName = it
            }
        }

        /**
         * 获取是否首次启动标识
         */
        @Synchronized
        fun getAppFirstLaunch(context: Context): Int {
            if (appFirstLaunch == InsightInto.DEF_INT) {
                tryI {
                    appFirstLaunch = SpPrefs.getInt(context, "AppFirstLaunch", 1)
                    if (appFirstLaunch == 1) {
                        SpPrefs.putInt(context, "AppFirstLaunch", 0)
                    }
                }
            }
            return appFirstLaunch
        }

        /**
         * 构建请求头公共参数验签
         * 1、Key先转大写
         * 2、对 Key 排序
         * 3、拼接 key=value&key=value...
         * 4、对拼接字符串 sha256
         *
         * @param commonHeaders
         * @return
         */
        fun createSign(commonHeaders: Map<String, String>): String {
            val map: MutableMap<String, String> = HashMap()
            val list: MutableList<String> = ArrayList()
            //1、Key先转大写
            for ((key, value) in commonHeaders) {
                val uppperKey = key.uppercase()
                list.add(uppperKey)
                map[uppperKey] = value
            }

            //2、对 Key 排序
            list.sortWith { o1, o2 -> o1.compareTo(o2) }
            //3、拼接 key=value&key=value...
            val signSb = StringBuilder()
            for (key in list) {
                var value = map[key]
                if (signSb.isNotEmpty()) {
                    signSb.append("&")
                }
                if (value == null) {
                    value = ""
                }
                signSb.append(key)
                signSb.append("=")
                signSb.append(value)
            }

            val signSha256 = signSb.toString().toSha256()
            //4、对拼接字符串进行 sha256
            return signSha256
        }

        /**
         * 获取当前应用 PackageInfo
         */
        private fun getAppPackageInfo(context: Context): PackageInfo {
            return appPackageInfo ?: context.run {
                packageManager.getPackageInfo(context.packageName, 0).also {
                    appPackageInfo = it
                }
            }
        }

        /**
         * 构建 AES_KEY
         */
        private fun getAkxStr(): String {
            val list = listOf(
                "c",
                "4",
                "5",
                "6",
                "8",
                "9",
                "3",
                "c",
                "e",
                "f",
                "1",
                "f",
                "c",
                "8",
                "3",
                "1"
            )
            return list.joinToString(separator = "")
        }

        /**
         * 获取 Adid 必须异步调用，不能在主线程中执行
         */
        @Synchronized
        fun getAdidInfo(context: Context): AdidData {
            try {
                val currentTime = System.currentTimeMillis()
                if (currentTime - adidReadTime > 5 * 60 * 1000) {
                    val idInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
                    adidResult.isLimitAdTrackingEnabled =
                        idInfo.isLimitAdTrackingEnabled.toInteger()
                    if (!TextUtils.isEmpty(idInfo.id)) {
                        adidReadTime = currentTime
                        adidResult.adid = idInfo.id!!
                    } else {
                        adidResult.error = "Get google adid is empty!"
                        ILog.e(adidResult.error)
                    }
                }
            } catch (e: GooglePlayServicesNotAvailableException) {
                adidResult.error =
                    "Get google adid not available exception:${e.message}"
                ILog.e(adidResult.error, e)
            } catch (e: GooglePlayServicesRepairableException) {
                //Google 可修复异常，设备 Google Play 服务更新后可能会解决
                adidResult.error =
                    "Get google adid repairable exception:${e.message}"
                ILog.e(adidResult.error, e)
            } catch (e: Exception) {
                adidResult.error =
                    "Get google adid other exception:${e.message}"
                ILog.e(adidResult.error, e)
            }

            return adidResult
        }

        /**
         * 获取设备匿名标识符
         */
        @Synchronized
        internal fun getAnonymityId(context: Context): AnonymityId {
            val androidId = getAndroidId(context)
            val deviceModel = getBuildModel()
            val generateAlgorithm: Int

            val anonymityIdStr =
                if (TextUtils.isEmpty(androidId) || androidIdBlackList.contains(androidId)) {
                    //androidId命中黑名单，使用随机 UUID 作为设备匿名标识符，同时持久化存储
                    val anonymityIdKey = "$deviceModel-AnonymityId"
                    generateAlgorithm = 2
                    val anonymityIdCache = SpPrefs.getAesStr(context, anonymityIdKey)
                    if (TextUtils.isEmpty(anonymityIdCache)) {
                        createRandomUUID().also {
                            SpPrefs.putAesStr(
                                context,
                                anonymityIdKey,
                                it
                            )
                        }
                    } else {
                        anonymityIdCache
                    }
                } else {
                    generateAlgorithm = 0
                    androidId
                }

            //生成Md5摘要信息
            val anonymityIdMd5 = anonymityIdStr.toMd5().lowercase()
            //添加校验位
            val id = appendCheckMark(anonymityIdMd5)
            val info = AnonymityId(
                id = id,
                createTime = System.currentTimeMillis(),
                createAlgorithm = generateAlgorithm
            )
            return info
        }

    }
}