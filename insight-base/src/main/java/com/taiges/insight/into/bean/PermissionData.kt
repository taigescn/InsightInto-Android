package com.taiges.insight.into.bean

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.taiges.insight.into.common.fromJsonObject
import com.taiges.insight.into.common.saveJsonObject
import com.taiges.insight.into.common.toInteger

/**
 * 权限信息
 */
class PermissionData private constructor(
    val eventProperties: Map<String, Any>,
    val permissions: Array<out String>
) {

    companion object {

        private const val KEY_PERMISSION_RECORD = "PermissionRecord"
        private val DEFAULT_PERMISSION_RECORD = PermissionRecord()

        fun create(
            context: Context,
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray,
        ): PermissionData {
            val data = mutableMapOf<String, Any>()
            data["requestCode"] = requestCode
            var hasCoarseLocation = false
            var hasNotifications = false
            var hasCamera = false
            val otherPermissions = mutableListOf<String>()

            //取出上次存储结果
            val oldPermissionRecord = fromJsonObject(
                context, KEY_PERMISSION_RECORD
            ) ?: DEFAULT_PERMISSION_RECORD
            data["firstRequestPermission"] =
                if (oldPermissionRecord == DEFAULT_PERMISSION_RECORD) 1 else 0

            for (i in permissions.indices) {
                val permission = permissions[i]
                val grantResult = grantResults[i]
                val isGranted = grantResult == PackageManager.PERMISSION_GRANTED

                when (permission) {
                    Manifest.permission.CAMERA -> {
                        hasCamera = isGranted
                        data["hasCamera"] = isGranted.toInteger()
                        data["changeCamera"] = (hasCamera != oldPermissionRecord.hasCamera).toInteger()
                    }

                    Manifest.permission.ACCESS_COARSE_LOCATION -> {
                        hasCoarseLocation = isGranted
                        data["hasCoarseLocation"] = isGranted.toInteger()
                        data["changeCoarseLocation"] =
                            (hasCoarseLocation != oldPermissionRecord.hasCoarseLocation).toInteger()
                    }

                    "android.permission.POST_NOTIFICATIONS" -> {
                        hasNotifications = isGranted
                        data["hasNotifications"] = isGranted.toInteger()
                        data["changeNotifications"] =
                            (hasNotifications != oldPermissionRecord.hasNotifications).toInteger()
                    }

                    else -> {
                        otherPermissions.add("$permission(${isGranted.toInteger()})")
                    }
                }
            }

            if (otherPermissions.isNotEmpty()) {
                data["otherPermissions"] =
                    otherPermissions.joinToString("|")
            }

            val permissionRecord =
                PermissionRecord(
                    hasCoarseLocation = hasCoarseLocation,
                    hasNotifications = hasNotifications,
                    hasCamera = hasCamera
                )

            //缓存当前授权结果
            saveJsonObject(context, KEY_PERMISSION_RECORD, permissionRecord)

            return PermissionData(data, permissions)
        }
    }

    internal class PermissionRecord(
        val hasCoarseLocation: Boolean = false,
        var hasNotifications: Boolean = false,
        val hasCamera: Boolean = false
    )

    fun hasLocationPermission() = permissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION)

}