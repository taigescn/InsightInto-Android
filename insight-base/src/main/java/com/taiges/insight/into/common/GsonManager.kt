package com.taiges.insight.into.common

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/**
 * Gson 管理类
 */
object GsonManager {
    val gson: Gson by lazy {
        GsonBuilder()
            .enableComplexMapKeySerialization()
            .registerTypeAdapter(
                object : TypeToken<MutableMap<String, Any?>>() {}.type, NumberTypeAdapter()
            ).create()
    }

    /**
     * Map<String,Any?> 类型反序列化数值类型适配器
     */
    internal class NumberTypeAdapter : JsonDeserializer<Map<String, Any?>> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext,
        ): Map<String, Any?> {
            return parseJsonElement(json) as Map<String, Any?>
        }

        private fun parseJsonElement(element: JsonElement): Any? {
            return when {
                element.isJsonObject -> {
                    val obj = element.asJsonObject
                    obj.entrySet().associate { (key, value) ->
                        key to parseJsonElement(value)
                    }
                }

                element.isJsonArray -> {
                    element.asJsonArray.map { parseJsonElement(it) }
                }

                element.isJsonPrimitive -> {
                    val primitive = element.asJsonPrimitive
                    when {
                        primitive.isString -> primitive.asString
                        primitive.isBoolean -> primitive.asBoolean
                        primitive.isNumber -> parseNumber(primitive.asNumber)
                        else -> null
                    }
                }

                element.isJsonNull -> null
                else -> null
            }
        }

        private fun parseNumber(number: Number): Number {
            val str = number.toString()
            return when {
                str.contains('.') -> {
                    number.toDouble()
                }

                else -> {
                    number.toLong()
                }
            }
        }
    }
}