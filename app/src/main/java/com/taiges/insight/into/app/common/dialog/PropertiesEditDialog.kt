package com.taiges.insight.into.app.common.dialog

import com.taiges.insight.into.app.databinding.DialogPropertiesEditBinding
import com.taiges.insight.into.app.ui.activity.BaseBindingActivity

/**
 * 属性编辑 Dialog
 */
class PropertiesEditDialog(
    activity: BaseBindingActivity<*>,
    private val addBlock: (Pair<String, Any>) -> Unit,
) :
    BaseBindingDialog<DialogPropertiesEditBinding>(activity) {

    override fun onCreateBinding() = DialogPropertiesEditBinding.inflate(layoutInflater)

    init {
        binding.addTv.setOnClickListener {
            val key = binding.propertiesKeyEt.text.toString()
            val value = binding.propertiesValueEt.text.toString()
            if (key.isEmpty()) {
                activity.showToast("属性 Key 必填!")
                return@setOnClickListener
            }

            addBlock.invoke(key to value)
            dismiss()
        }

        binding.cancelTv.setOnClickListener {
            dismiss()
        }

        setOnDismissListener {
            binding.propertiesKeyEt.setText("")
            binding.propertiesValueEt.setText("")
        }
    }
}