package com.nitix.otimizador

import android.content.Context
import android.content.SharedPreferences

object Store {
    private lateinit var sp: SharedPreferences
    fun init(c: Context) {
        if (!::sp.isInitialized) sp = c.applicationContext.getSharedPreferences("nitix", Context.MODE_PRIVATE)
    }
    fun getBool(k: String) = sp.getBoolean(k, false)
    fun getStr(k: String) = sp.getString(k, "") ?: ""
    fun getInt(k: String, d: Int) = sp.getInt(k, d)
    fun put(k: String, v: Boolean) = sp.edit().putBoolean(k, v).apply()
    fun put(k: String, v: String) = sp.edit().putString(k, v).apply()
    fun put(k: String, v: Int) = sp.edit().putInt(k, v).apply()
}
