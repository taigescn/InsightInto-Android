package com.taiges.insight.into.bean.api

/**
 * @param itemId 采集项 id
 * @param name 采集项名称
 * @param isFollowEvent 是否跟随事件上报该采集项，0：否，1：是
 * @param isTimeConsuming 是否为耗时采集项，0：否，1：是
 * @param cacheTime 采集项缓存时间(毫秒)
 * @param uploadIntervalTime 采集项采集上报间隔时间(毫秒)
 */
class ReaderConfig(
    val itemId: Int,
    val name: String,
    val isFollowEvent: Int,
    val isTimeConsuming: Int,
    val cacheTime: Long?,
    val uploadIntervalTime: Long?,
) {
    /**
     * 是否需要本地缓存数据
     */
    fun isCacheData() = cacheTime?.let { it > 0 } ?: false
}