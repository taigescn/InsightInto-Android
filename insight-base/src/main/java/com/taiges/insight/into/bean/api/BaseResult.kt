package com.taiges.insight.into.bean.api

import com.taiges.insight.into.InsightInto

/**
 * 服务端 Api 返回结果泛型类
 * @param code 服务端状态码,200 表示成功
 * @param message 服务端服务端描述信息
 * @param data 返回数据对象
 */
internal class BaseResult<T>(
    val code: Int,
    val message: String,
    val data: T? = null
) {

    companion object {

        inline fun <reified R> createError(msg: String): BaseResult<R> {
            return BaseResult(InsightInto.DEF_INT, msg)
        }
    }

    fun isSuccess() = code == 200

    fun isCheckFail() = code == 300 || code == 301
}