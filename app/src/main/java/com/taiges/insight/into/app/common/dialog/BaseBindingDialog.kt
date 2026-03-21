package com.taiges.insight.into.app.common.dialog

import android.app.Dialog
import android.view.Gravity
import androidx.viewbinding.ViewBinding
import com.taiges.insight.into.app.R
import com.taiges.insight.into.app.ui.activity.BaseBindingActivity

/**
 * Dialog 基类
 */
abstract class BaseBindingDialog<T : ViewBinding>(
    activity: BaseBindingActivity<*>,
    themeResId: Int = R.style.BaseDialog,
) : Dialog(activity, themeResId) {

    protected val binding: T by lazy {
        onCreateBinding()
    }

    init {
        setContentView(binding.root)
        setCanceledOnTouchOutside(false)
        setWindowParam()
    }

    abstract fun onCreateBinding(): T

    open fun setWindowParam() {
        window?.also {
            val attr = it.attributes
            attr.width = it.windowManager.defaultDisplay.width
            attr.gravity = Gravity.CENTER
            it.attributes = attr
        }
    }
}