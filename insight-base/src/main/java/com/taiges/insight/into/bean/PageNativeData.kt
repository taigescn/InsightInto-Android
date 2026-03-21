package com.taiges.insight.into.bean

/**
 * @param pageCurrentKey 当前页面实例Key
 * @param pageCurrentName 当前页面标识
 * @param pagePreviousKey 上一个页面实例Key
 * @param pagePreviousName 上一个页面标识
 * @param pageIsRoot 是否为根页面(RN特有)
 */
class PageNativeData(
    var pageCurrentKey: String? = "",
    var pageCurrentName: String? = "",
    var pagePreviousKey: String? = "",
    var pagePreviousName: String? = "",
    var pageIsRoot: String? = "",
) {

    fun getMapInfo(): Map<String, String> {
        return mapOf(
            "pageCurrentKey" to (pageCurrentKey ?: ""),
            "pageCurrentName" to (pageCurrentName ?: ""),
            "pagePreviousKey" to (pagePreviousKey ?: ""),
            "pagePreviousName" to (pagePreviousName ?: ""),
            "pageIsRoot" to (pageIsRoot ?: "")
        )
    }
}