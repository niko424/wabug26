package com.example.wabug

import android.content.Intent
import android.net.Uri
import android.util.Log

object Sender {
    fun open(num: String, text: String, pkg: String): Boolean {
        try {
            val i = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$num")).apply {
                putExtra("sms_body", text)
                putExtra(Intent.EXTRA_TEXT, text)
                setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            App.ctx.startActivity(i)
            return true
        } catch (t: Throwable) { Log.e("Sender", "sendto: ${t.message}") }

        try {
            val wa = "https://wa.me/$num?text=" + Uri.encode(text.take(1800))
            App.ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(wa)).apply {
                setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            return true
        } catch (t: Throwable) { Log.e("Sender", "wa.me: ${t.message}") }
        return false
    }
}
