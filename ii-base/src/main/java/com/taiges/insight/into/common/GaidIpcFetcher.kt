package com.taiges.insight.into.common

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Parcel
import com.taiges.insight.into.bean.AdidData
import java.util.concurrent.LinkedBlockingQueue

/**
 * 本地 IPC 方式获取 Google 广告 id
 */
internal object GaidIpcFetcher {

    /**
     * 获取 Google 广告 id
     */
    fun getGoogleAdid(context: Context): AdidData? {
        val intent = Intent("com.google.android.gms.ads.identifier.service.START").apply {
            setPackage("com.google.android.gms")
        }
        val connection = AdvertisingConnection()
        return try {
            // 绑定 Google Play Services 的本地 Service
            if (context.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
                val binder = connection.getBinder()
                val helper = AdvertisingInterface(binder)
                val adid = helper.getId()
                val isLimit = helper.isLimitAdTrackingEnabled().toInteger()
                adid?.let {
                    AdidData(adid, isLimit, "")
                }
            } else {
                null
            }
        } finally {
            tryI {
                context.unbindService(connection)
            }
        }
    }

    private class AdvertisingConnection : ServiceConnection {
        private val queue = LinkedBlockingQueue<IBinder>(1)

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            tryI {
                if (service != null) {
                    queue.put(service)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {}

        fun getBinder(): IBinder {
            return queue.take()
        }
    }

    private class AdvertisingInterface(private val binder: IBinder) {

        fun getId(): String? {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            return try {
                data.writeInterfaceToken("com.google.android.gms.ads.identifier.internal.IAdvertisingIdService")
                // transact(1) 对应 AIDL 中 getId() 的 Transaction ID
                binder.transact(1, data, reply, 0)
                reply.readException()
                reply.readString()
            } finally {
                reply.recycle()
                data.recycle()
            }
        }

        fun isLimitAdTrackingEnabled(): Boolean {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            return try {
                data.writeInterfaceToken("com.google.android.gms.ads.identifier.internal.IAdvertisingIdService")
                // 传 true(1) 进去作为参数
                data.writeInt(1)
                binder.transact(2, data, reply, 0)
                reply.readException()
                // 返回值为非 0 代表用户启用了“限制个性化广告”
                reply.readInt() != 0
            } finally {
                reply.recycle()
                data.recycle()
            }
        }
    }
}