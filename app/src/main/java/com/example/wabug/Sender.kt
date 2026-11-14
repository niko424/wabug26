package com.example.wabug
import android.content.Intent
import android.net.Uri
object Sender {
    fun open(num: String, text: String, pkg: String): Boolean {
        try {
            val i = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$num")).apply {
                putExtra("sms_body", text); setPackage(pkg); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            App.ctx.startActivity(i); return true
        } catch (t: Throwable) { return false }
    }
}
