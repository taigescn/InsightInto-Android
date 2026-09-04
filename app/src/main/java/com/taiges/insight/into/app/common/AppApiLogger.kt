package com.taiges.insight.into.app.common

import com.taiges.insight.into.log.ApiLogger

class AppApiLogger: ApiLogger() {

    override fun message(msg: String) {
        super.message(msg)
        MLog.d(msg)
    }
}