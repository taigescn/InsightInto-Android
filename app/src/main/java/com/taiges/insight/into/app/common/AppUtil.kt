package com.taiges.insight.into.app.common

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.taiges.insight.into.common.getAppName

/**
 * 应用工具类
 */
class AppUtil {

    companion object {

        private var targetSdkVersion = 0
        private var appName: String? = null
        private var appPackageInfo: PackageInfo? = null

        fun getSdkTargetVersion(context: Context): Int {
            if (targetSdkVersion == 0) {
                try {
                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    targetSdkVersion = packageInfo.applicationInfo.targetSdkVersion
                } catch (e: PackageManager.NameNotFoundException) {
                    MLog.e("getSdkTargetVersion exception!", e)
                }
            }
            return targetSdkVersion
        }

        /**
         * 获取App名称
         */
        fun getAppName(context: Context): String {
            return appName
                ?: getAppPackageInfo(context).getAppName(context).also {
                    appName = it
                }
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
    }
}