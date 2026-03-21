package com.taiges.insight.into.app.ui.adapter

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.taiges.insight.into.app.R

/**
 * 属性 Item Adapter
 */
class PropertiesAdapter(data: MutableList<Pair<String, Any>> = mutableListOf()) :
    BaseQuickAdapter<Pair<String, Any>, BaseViewHolder>(
        R.layout.item_properties,
        data
    ) {
    override fun convert(helper: BaseViewHolder, item: Pair<String, Any>) {
        helper.setText(R.id.kv_tv, "${item.first} : ${item.second}")
    }
}