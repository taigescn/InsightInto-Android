package com.taiges.insight.into.app.ui.activity

import android.os.Bundle
import com.taiges.insight.into.app.common.checkEmptyStr
import com.taiges.insight.into.app.databinding.ActivityDeviceInfoBinding
import com.taiges.insight.into.app.kit.InsightIntoKit
import kotlin.concurrent.thread

/**
 * 设备信息页面
 */
class DeviceInfoActivity : BaseBindingActivity<ActivityDeviceInfoBinding>() {

    override fun onCreateBinding() = ActivityDeviceInfoBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        thread {
            val adidData = InsightIntoKit.getAdid()
            runOnUiThread {
                binding.adidValueTv.text = adidData.adid.checkEmptyStr()
                binding.isLimitAdTrackingEnabledValueTv.text =
                    adidData.isLimitAdTrackingEnabled.toString()
            }
        }
    }
}
