package com.matrix.launcher

import android.content.SharedPreferences

/** Staged editor and strict getters model Android's atomic apply and wrong-type failures. */
internal fun mapBackedPrefs(map: MutableMap<String, Any> = mutableMapOf(), commitSucceeds: Boolean = true): SharedPreferences {
    fun editor(): SharedPreferences.Editor {
        val changes = mutableMapOf<String, Any?>()
        var clear = false
        lateinit var proxy: SharedPreferences.Editor
        proxy = java.lang.reflect.Proxy.newProxyInstance(SharedPreferences.Editor::class.java.classLoader,
            arrayOf(SharedPreferences.Editor::class.java)) { _, method, args ->
            when (method.name) {
                "putString", "putStringSet", "putInt", "putLong", "putFloat", "putBoolean" -> { changes[args[0] as String] = args[1]; proxy }
                "remove" -> { changes[args[0] as String] = null; proxy }
                "clear" -> { clear = true; proxy }
                "apply", "commit" -> {
                    if (clear) map.clear()
                    changes.forEach { (key, value) -> if (value == null) map.remove(key) else map[key] = value }
                    if (method.name == "commit") commitSucceeds else null
                }
                else -> null
            }
        } as SharedPreferences.Editor
        return proxy
    }
    return java.lang.reflect.Proxy.newProxyInstance(SharedPreferences::class.java.classLoader,
        arrayOf(SharedPreferences::class.java)) { _, method, args ->
        when (method.name) {
            "getAll" -> map.toMap()
            "contains" -> map.containsKey(args[0] as String)
            "getString", "getStringSet", "getInt", "getLong", "getFloat", "getBoolean" -> {
                val value = map[args[0] as String] ?: return@newProxyInstance args[1]
                val valid = when (method.name) {
                    "getString" -> value is String
                    "getStringSet" -> value is Set<*>
                    "getInt" -> value is Int
                    "getLong" -> value is Long
                    "getFloat" -> value is Float
                    else -> value is Boolean
                }
                if (!valid) throw ClassCastException("Wrong preference type")
                value
            }
            "edit" -> editor()
            else -> null
        }
    } as SharedPreferences
}
