package com.taiges.insight.into.app.ui.activity

import android.Manifest
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.taiges.insight.into.app.R
import com.taiges.insight.into.app.common.AccountManager
import com.taiges.insight.into.app.common.AppSpUtil
import com.taiges.insight.into.app.common.MLog
import com.taiges.insight.into.app.common.dialog.PropertiesEditDialog
import com.taiges.insight.into.app.common.hasOneNotPermissions
import com.taiges.insight.into.app.databinding.ActivityEventBinding
import com.taiges.insight.into.app.kit.InsightIntoKit
import com.taiges.insight.into.app.ui.adapter.PropertiesAdapter

/**
 * 事件测试页面
 */
class EventActivity : BaseBindingActivity<ActivityEventBinding>() {

    private val eventPropertiesAdapter = PropertiesAdapter()
    private val sessionPropertiesAdapter = PropertiesAdapter(InsightIntoKit.getSessionProperties())

    private val eventPropertiesEditDialog by lazy {
        PropertiesEditDialog(this) {
            eventPropertiesAdapter.addData(it)
        }
    }

    private val sessionPropertiesEditDialog by lazy {
        PropertiesEditDialog(this) {
            InsightIntoKit.addSessionProperties(mapOf(it))
            sessionPropertiesAdapter.addData(it)
        }
    }

    override fun onCreateBinding() = ActivityEventBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AccountManager.getUserId().let {
            binding.userIdEt.setText(it)
        }

        binding.settingUserIdBtn.setOnClickListener {
            val userId = binding.userIdEt.text.toString()
            AccountManager.setUserId(userId)
            sessionPropertiesAdapter.notifyDataSetChanged()
        }

        AccountManager.getUniqueId().let {
            binding.uniqueIdEt.setText(it)
        }

        binding.settingUniqueIdBtn.setOnClickListener {
            val uniqueId = binding.uniqueIdEt.text.toString()
            AccountManager.setUniqueId(uniqueId)
        }

        AccountManager.getPhoneNumber().let {
            binding.phoneNumberEt.setText(it)
        }

        binding.settingPhoneNumberBtn.setOnClickListener {
            val phoneNumber = binding.phoneNumberEt.text.toString()
            AccountManager.setPhoneNumber(phoneNumber)
        }

        binding.requestPermissionBtn.setOnClickListener {
            val permissionList = mutableListOf<String>()
            permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            permissionList.add(Manifest.permission.READ_SMS)

            val permissions = permissionList.toTypedArray()

            if (hasOneNotPermissions(*permissions)) {
                ActivityCompat.requestPermissions(
                    this,
                    permissions,
                    1
                )
            }
        }

        val cacheEcode = AppSpUtil.getString(this, "EcodeKey", "TestAll")
        binding.eventEt.setText(cacheEcode)
        binding.sendEventBtn.setOnClickListener {
            val ecode = binding.eventEt.text.toString()
            if (ecode.isEmpty()) {
                showToast("事件编码不能为空!")
                return@setOnClickListener
            }

            AppSpUtil.putString(this, "EcodeKey", ecode)
            InsightIntoKit.sendEvent(ecode, eventPropertiesAdapter.data.toMap())
        }

        binding.forwardH5Btn.setOnClickListener {
            forword(WebViewActivity::class.java)
        }

        binding.privacyPolicyCb.isChecked = AccountManager.isPrivacyPolicy()
        binding.privacyPolicyCb.setOnCheckedChangeListener { _, isChecked ->
            InsightIntoKit.setPrivacyPolicy(isChecked)
            AccountManager.setPrivacyPolicy(isChecked)
        }

        binding.eventPropertiesAddBtn.setOnClickListener {
            eventPropertiesEditDialog.show()
        }

        binding.sessionPropertiesAddBtn.setOnClickListener {
            sessionPropertiesEditDialog.show()
        }

        binding.eventPropertiesRecyclerView.also {
            it.adapter = eventPropertiesAdapter
            it.layoutManager = LinearLayoutManager(this)

            eventPropertiesAdapter.addChildClickViewIds(R.id.delete_iv)
            eventPropertiesAdapter.setOnItemChildClickListener { _, view, position ->
                when (view.id) {
                    R.id.delete_iv -> {
                        eventPropertiesAdapter.remove(position)
                    }
                }
            }
        }

        binding.sessionPropertiesRecyclerView.also {
            it.adapter = sessionPropertiesAdapter
            it.layoutManager = LinearLayoutManager(this)

            sessionPropertiesAdapter.addChildClickViewIds(R.id.delete_iv)
            sessionPropertiesAdapter.setOnItemChildClickListener { _, view, position ->
                val pair = sessionPropertiesAdapter.getItem(position)
                when (view.id) {
                    R.id.delete_iv -> {
                        sessionPropertiesAdapter.remove(position)
                        InsightIntoKit.removeSessionProperties(listOf(pair.first))
                    }
                }
            }
        }

        val apiHeaders = InsightIntoKit.getApiHeaders()
        MLog.d("apiHeaders:${Gson().toJson(apiHeaders)}")
    }
}