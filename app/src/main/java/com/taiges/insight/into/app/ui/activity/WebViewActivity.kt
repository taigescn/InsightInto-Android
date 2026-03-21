package com.taiges.insight.into.app.ui.activity

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.webkit.*
import androidx.core.app.ActivityCompat
import com.taiges.insight.into.app.databinding.ActivityWebViewBinding
import com.taiges.insight.into.app.kit.InsightIntoKit
import com.taiges.insight.into.app.common.MLog
import com.taiges.insight.into.app.common.AccountManager
import com.taiges.insight.into.app.common.getCurrentKey
import com.taiges.insight.into.app.common.hasOneNotPermissions

/**
 * WebView 测试页面
 */
class WebViewActivity : BaseBindingActivity<ActivityWebViewBinding>() {
    private var geolocationPermissionsOrigin: String? = null
    private var geolocationPermissionsCallback: GeolocationPermissions.Callback? = null

    override fun onCreateBinding() = ActivityWebViewBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = binding.webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.allowContentAccess = true
        settings.allowFileAccess = true
        settings.cacheMode = WebSettings.LOAD_NO_CACHE

        //文件访问
        settings.allowContentAccess = true
        settings.allowFileAccess = true
        settings.allowFileAccessFromFileURLs = true

        //显示
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(false)
        settings.displayZoomControls = false
        settings.defaultTextEncodingName = "UTF-8"

        settings.setGeolocationEnabled(true)
        settings.setSupportMultipleWindows(true)
        settings.saveFormData = true
        binding.webView.removeJavascriptInterface("searchBoxJavaBridge_")
        //允许跨域
        settings.allowUniversalAccessFromFileURLs = true

        //注入 Demo 桥接交互 Api
        binding.webView.addJavascriptInterface(NativeBridgeApi(), "DemoBridgeApi")

        binding.webView.webChromeClient = object : WebChromeClient() {

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?, callback: GeolocationPermissions.Callback?,
            ) {
                super.onGeolocationPermissionsShowPrompt(origin, callback)
                MLog.d("onGeolocationPermissionsShowPrompt origin:$origin")
                val permissions = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
                if (hasOneNotPermissions(*permissions)) {
                    geolocationPermissionsOrigin = origin
                    geolocationPermissionsCallback = callback
                    ActivityCompat.requestPermissions(this@WebViewActivity, permissions, 1)
                } else {
                    AlertDialog.Builder(this@WebViewActivity).apply {
                        setTitle("获取地理位置")
                        setMessage("$origin 需要获取您的地理位置信息，是否允许？")
                        setPositiveButton("允许") { _, _ ->
                            callback?.invoke(origin, true, true)
                        }
                        setNegativeButton("取消") { _, _ ->
                            callback?.invoke(origin, false, true)
                        }
                        setCancelable(false)
                        create().show()
                    }
                }
            }

            override fun onJsAlert(
                view: WebView?, url: String?, message: String?, result: JsResult?,
            ): Boolean {
                AlertDialog.Builder(this@WebViewActivity).apply {
                    setTitle("Alert")
                    setMessage(message)
                    setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
                    setCancelable(false)
                    create().show()
                }
                return true
            }

            override fun onJsConfirm(
                view: WebView?, url: String?, message: String?, result: JsResult?,
            ): Boolean {
                AlertDialog.Builder(this@WebViewActivity).apply {
                    setTitle("Confirm")
                    setMessage(message)
                    setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
                    setNegativeButton(android.R.string.cancel) { _, _ -> result?.cancel() }
                    setCancelable(false)
                    create().show()
                }
                return true
            }

            override fun onJsPrompt(
                view: WebView?,
                url: String?,
                message: String?,
                defaultValue: String?,
                result: JsPromptResult?,
            ): Boolean {
                AlertDialog.Builder(this@WebViewActivity).apply {
                    setTitle("Prompt")
                    setMessage(message)
                    setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm("未实现输入") }
                    setNegativeButton(android.R.string.cancel) { _, _ -> result?.cancel() }
                    setCancelable(false)
                    create().show()
                }
                return true
            }
        }

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?,
            ) {
                handler?.proceed()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.let {
                    val pageUrl = url ?: ""
                    val uri = Uri.parse(pageUrl)
                    val pagePath = uri.path ?: ""
                    val pageTitle = it.title ?: ""
                    InsightIntoKit.setPageWebViewData(
                        this@WebViewActivity.getCurrentKey(),
                        pageUrl,
                        pagePath,
                        pageTitle
                    )
                }
            }
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.removeSessionCookie()
        cookieManager.removeAllCookie()

        val userId = AccountManager.getUserId()
        val url = "https://www.insight-into.io"
        cookieManager.setCookie(url, "userId=$userId")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.flush()
        } else {
            CookieSyncManager.getInstance().sync()
        }

        binding.webView.loadUrl(url)
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty()) {
            val allow = PackageManager.PERMISSION_GRANTED == grantResults[0]
            geolocationPermissionsCallback?.invoke(geolocationPermissionsOrigin, allow, true)
        }
        geolocationPermissionsOrigin = null
        geolocationPermissionsCallback = null
    }

    override fun onDestroy() {
        super.onDestroy()
        InsightIntoKit.removePageWebViewData(getCurrentKey())
    }

    private inner class NativeBridgeApi {

        @JavascriptInterface
        fun goBack() {
            runOnUiThread {
                onBackPressed()
            }
        }
    }

}