package com.taiges.insight.into.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * 事件/采集数据线程切换通道
 */
internal class ChannelBus<T>(capacity: Int = Channel.UNLIMITED) {

    private val channel = Channel<T>(capacity = capacity)

    /**
     * 发送数据
     */
    fun send(data: T) {
        BusScope().launch {
            tryBiz("send exception! data:$data") {
                channel.send(data)
            }
        }
    }

    /**
     * 接收数据，回调在 IO 线上中执行
     */
    fun receive(block: suspend CoroutineScope.(t: T) -> Unit) {
        BusScope().launch {
            tryBiz("receive exception!") {
                for (data in channel) {
                    tryBiz("receive data exception!") {
                        block(data)
                    }
                }
            }
        }
    }

    /**
     * Channel IO 作用域
     */
    private class BusScope : CoroutineScope {
        override val coroutineContext: CoroutineContext = Dispatchers.IO + SupervisorJob()
    }
}



