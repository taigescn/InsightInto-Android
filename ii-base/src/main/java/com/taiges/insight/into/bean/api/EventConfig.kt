package com.taiges.insight.into.bean.api

/**
 * 事件 Item 配置
 * @param ecode 事件 code
 * @param isRealTime 是否为实时事件，0：否，1：是
 * @param items 采集项字符串数组
 */
class EventConfig(val ecode: String, isRealTime: Int, val items: String?)