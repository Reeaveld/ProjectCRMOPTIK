package com.optik.cengkareng.core.network

import com.google.gson.JsonElement
import com.google.gson.JsonParser

/**
 * Menerjemahkan body error Laravel menjadi satu pesan yang dapat ditampilkan ke pengguna.
 *
 * Dua format yang dikenal:
 *  - `{"success":false,"message":"Validasi Gagal","errors":{"no_hp":["..."]}}` (CustomerController)
 *  - `{"message":"The amount field is required.","errors":{"amount":["..."]}}` (validasi Laravel standar)
 *
 * Prioritas: pesan pertama di `errors` → `message` → `fallback`.
 * Implementasi murni (Gson) agar dapat dites tanpa Robolectric.
 */
object ApiErrorParser {

    fun parse(errorBody: String?, fallback: String): String {
        if (errorBody.isNullOrBlank()) return fallback

        val root = try {
            JsonParser.parseString(errorBody)
        } catch (_: Exception) {
            return fallback
        }
        if (!root.isJsonObject) return fallback
        val obj = root.asJsonObject

        firstFieldError(obj.get("errors"))?.let { return it }

        val message = obj.get("message")
        if (message != null && message.isJsonPrimitive) {
            val text = message.asString
            if (text.isNotBlank()) return text
        }
        return fallback
    }

    private fun firstFieldError(errors: JsonElement?): String? {
        if (errors == null || !errors.isJsonObject) return null
        for ((_, value) in errors.asJsonObject.entrySet()) {
            when {
                value.isJsonArray -> {
                    val first = value.asJsonArray.firstOrNull { it.isJsonPrimitive }
                    if (first != null && first.asString.isNotBlank()) return first.asString
                }
                value.isJsonPrimitive && value.asString.isNotBlank() -> return value.asString
            }
        }
        return null
    }
}
