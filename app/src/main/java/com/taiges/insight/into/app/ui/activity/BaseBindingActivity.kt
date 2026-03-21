package com.taiges.insight.into.app.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.taiges.insight.into.app.kit.InsightIntoKit

/**
 * Binding Activity 基类
 */
abstract class BaseBindingActivity<T : ViewBinding> : AppCompatActivity() {

    protected val binding: T by lazy {
        onCreateBinding()
    }

    protected abstract fun onCreateBinding(): T

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }

    /**
     * 显示Toast
     * @param resId 文本资源Id
     */
    fun showToast(resId: Int) {
        showToast(getString(resId))
    }

    /**
     * 显示Toast
     * @param msg 文本
     */
    fun showToast(msg: String) {
        runOnUiThread {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 跳转
     * @param cls 类
     */
    fun forword(cls: Class<*>) {
        forword(Intent(this, cls))
    }

    /**
     * 跳转
     * @param intent
     */
    fun forword(intent: Intent) {
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        InsightIntoKit.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}