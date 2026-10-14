package com.taiges.insight.into.bean

import android.content.pm.PackageManager
import com.taiges.insight.into.common.toInteger

/**
 * 权限信息
 */
class PermissionData(
    val eventProperties: Map<String, Any>,
    val permissions: Array<out String>
) {

    companion object {

        fun create(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray,
        ): PermissionData {
            val data = mutableMapOf<String, Any>()
            data["requestCode"] = requestCode
            val otherPermissions = mutableListOf<String>()

            for (i in permissions.indices) {
                val permission = permissions[i]
                val grantResult = grantResults[i]
                val isGranted = grantResult == PackageManager.PERMISSION_GRANTED
                otherPermissions.add("$permission(${isGranted.toInteger()})")
            }

            if (otherPermissions.isNotEmpty()) {
                data["otherPermissions"] =
                    otherPermissions.joinToString("|")
            }
            return PermissionData(data, permissions)
        }
    }

}