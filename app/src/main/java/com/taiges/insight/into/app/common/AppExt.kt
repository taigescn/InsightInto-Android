package com.taiges.insight.into.app.common

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

fun Any.getCurrentKey(): String {
    return "${this::class.java.name}@${Integer.toHexString(hashCode())}"
}

fun Context.hasOneNotPermissions(vararg permissions: String): Boolean {
    for (p in permissions) {
        if (!hasPermission(p)) {
            return true
        }
    }
    return false
}

fun Context.hasPermission(permission: String): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}