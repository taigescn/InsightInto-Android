package com.taiges.insight.into.common

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.os.Looper
import android.text.TextUtils
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.taiges.insight.into.common.log.ILog
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.concurrent.thread

/**
 * 扩展文件
 */

/**
 * SHA-256摘要
 */
fun String.toSha256(): String {
    val messageDigest: MessageDigest
    var encdeStr = ""
    try {
        messageDigest = MessageDigest.getInstance("SHA-256")
        val hash = messageDigest.digest(toByteArray(charset("UTF-8")))
        encdeStr = hash.toHex()
    } catch (e: NoSuchAlgorithmException) {
        e.printStackTrace()
    } catch (e: UnsupportedEncodingException) {
        e.printStackTrace()
    }
    return encdeStr
}

/**
 * 将byte转为16进制
 * @return
 */
fun ByteArray.toHex(): String {
    val sb = StringBuilder()
    //转成16进制
    this.forEach {
        val value = it
        val hex = value.toInt() and (0xFF)
        val hexStr = Integer.toHexString(hex)
        if (hexStr.length == 1) {
            sb.append(0).append(hexStr)
        } else {
            sb.append(hexStr)
        }
    }
    return sb.toString()
}

/**
 * 字符串 MD5
 */
fun String.toMd5() = toByteArray().toMd5()

/**
 * Byte 数组 MD5
 */
fun ByteArray.toMd5(): String {
    val sb = StringBuilder()
    val digest = MessageDigest.getInstance("MD5")
    val hashes = digest.digest(this)
    for (hash in hashes) {
        val hashHex = 0xff and hash.toInt()
        if (hashHex < 0x10) {
            sb.append('0').append(Integer.toHexString(hashHex))
        } else {
            sb.append(Integer.toHexString(hashHex))
        }
    }
    return sb.toString()
}

/**
 * Boolean 转 Int
 */
fun Boolean.toInteger() = if (this) 1 else 0

/**
 * 检查权限
 */
fun Context.hasPermission(permission: String): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

/**
 * 检查权限
 */
fun Context.hasArrayPermissions(vararg permissions: String): Boolean {
    for (p in permissions) {
        if (hasPermission(p)) {
            return true
        }
    }
    return false
}

fun Closeable.tryClose() {
    try {
        close()
    } catch (e: IOException) {
        ILog.e("tryClose Closeable exception!", e)
    }
}

/**
 * 捕获异常执行，异常则调用 biz 日志
 */
inline fun tryBiz(msg: String = "", block: () -> Unit) {
    try {
        block()
    } catch (t: Throwable) {
        ILog.buns(msg, t)
    }
}

/**
 * 捕获异常执行，忽略异常
 */
inline fun tryI(msg: String? = null, block: () -> Unit) {
    try {
        block()
    } catch (t: Throwable) {
        msg?.let {
            logE(it, t)
        }
    }
}

/**
 * 错误日志输出
 */
fun logE(msg: String, t: Throwable? = null) {
    ILog.e(msg, t)
}

/**
 * 时间戳格式化为指定格式，默认为：yyyy-MM-dd HH:mm:ss.SSS
 */
fun Long.formatTime(partter: String = "yyyy-MM-dd HH:mm:ss.SSS"): String {
    val sdf = SimpleDateFormat(partter)
    return sdf.format(Date(this))
}

/**
 * 获取安装列表应用名称
 */
fun PackageInfo.getAppName(context: Context): String {
    var name = ""
    tryI {
        applicationInfo?.also {
            name = it.loadLabel(context.packageManager).toString()
        }
    }
    return name
}

/**
 * 缓存 Json 对象
 */
internal fun saveJsonObject(context: Context, key: String, any: Any) {
    val value = Gson().toJson(any)
    SpPrefs.putAesStr(context, key, value)
}

/**
 * 读取 Json 缓存对象
 */
internal inline fun <reified T> fromJsonObject(context: Context, key: String): T? {
    try {
        val json = SpPrefs.getAesStr(context, key)
        if (!TextUtils.isEmpty(json)) {
            val type = object : TypeToken<T>() {}.type
            return Gson().fromJson(json, type)
        }
    } catch (_: Throwable) {
    }
    return null
}

/**
 * 尝试关闭输出流
 */
fun OutputStream.tryClose() {
    tryI {
        close()
    }
}

/**
 * 尝试关闭输入流
 */
fun InputStream.tryClose() {
    tryI {
        close()
    }
}

/**
 * 尝试关闭 Http 连接
 */
fun HttpURLConnection.tryDisconnect(){
    tryI {
        disconnect()
    }
}

/**
 * Cursor 获取对应字段 Int 类型数据
 */
fun Cursor.getIntData(columnName: String): Int? {
    return getIntOrNull(getColumnIndex(columnName))
}

/**
 * Cursor 获取对应字段 Long 类型数据
 */
fun Cursor.getLongData(columnName: String): Long? {
    return getLongOrNull(getColumnIndex(columnName))
}

/**
 * Cursor 获取对应字段字符串类型数据
 */
fun Cursor.getStringData(columnName: String): String {
    return getStringOrNull(getColumnIndex(columnName)) ?: ""
}

/**
 * 主线程运行
 */
fun threadOnMain(block: () -> Unit) {
    if (Looper.getMainLooper() == Looper.myLooper()) {
        thread {
            block()
        }
    } else {
        block()
    }
}