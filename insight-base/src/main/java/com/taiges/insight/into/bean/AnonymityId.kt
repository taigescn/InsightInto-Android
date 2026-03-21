package com.taiges.insight.into.bean


/**
 * 设备匿名标识符(设备指纹)
 * @param id 设备匿名标识符
 * @param createTime 生成时间戳
 * @param createAlgorithm 设备匿名标识符生成规则标识
 */
class AnonymityId(
    val id: String, val createTime: Long, var createAlgorithm: Int
)