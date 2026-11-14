package com.example.wabug
import android.content.Context

object Config {
    private const val PREFS = "wabug"
    private fun p(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun lastNumber(c: Context) = p(c).getString("lastNumber", "") ?: ""
    fun saveLastNumber(c: Context, v: String) = p(c).edit().putString("lastNumber", v).apply()

    fun lastPayload(c: Context) = p(c).getString("lastPayload", Payloads.ALL.first().id) ?: Payloads.ALL.first().id
    fun saveLastPayload(c: Context, v: String) = p(c).edit().putString("lastPayload", v).apply()

    fun delayMs(c: Context) = p(c).getLong("delayMs", 3000L)
    fun saveDelay(c: Context, v: Long) = p(c).edit().putLong("delayMs", v).apply()

    fun pkgIdx(c: Context) = p(c).getInt("pkgIdx", 0)
    fun savePkgIdx(c: Context, v: Int) = p(c).edit().putInt("pkgIdx", v).apply()

    fun autoSend(c: Context) = p(c).getBoolean("autoSend", true)
    fun saveAutoSend(c: Context, v: Boolean) = p(c).edit().putBoolean("autoSend", v).apply()
}
