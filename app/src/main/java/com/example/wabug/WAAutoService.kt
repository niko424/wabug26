package com.example.wabug

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class WAAutoService : AccessibilityService() {

    data class Job(val number: String, val payloadId: String, val pkg: String)

    companion object {
        @Volatile var instance: WAAutoService? = null
        @Volatile var delayMs = 3000L

        private val queue = ArrayDeque<Job>()
        private var total = 0
        private var done = 0
        private var awaitingSend = false
        private var auto = false
        private var progress: ((Int, Int, String) -> Unit)? = null

        fun enqueue(jobs: List<Job>, autoSend: Boolean, cb: (Int, Int, String) -> Unit) {
            queue.clear(); jobs.forEach { queue.addLast(it) }
            total = jobs.size; done = 0; awaitingSend = false
            auto = autoSend; progress = cb
            fireNext()
        }

        private fun fireNext() {
            val job = queue.removeFirstOrNull()
            if (job == null) {
                progress?.invoke(done, total, "selesai")
                awaitingSend = false
                return
            }
            val payload = Payloads.get(job.payloadId)?.gen() ?: "x"
            val ok = Sender.open(job.number, payload, job.pkg)
            if (!ok) {
                progress?.invoke(done, total, "gagal buka ${job.pkg}")
                done++
                Handler(Looper.getMainLooper()).postDelayed({ fireNext() }, 800L)
                return
            }
            if (auto && instance != null) {
                awaitingSend = true
                progress?.invoke(done + 1, total, "kirim ${job.number}")
                Handler(Looper.getMainLooper()).postDelayed({
                    if (awaitingSend) {
                        Log.w("WAAuto", "timeout: ${job.number}")
                        awaitingSend = false; done++
                        fireNext()
                    }
                }, 8000L)
            } else {
                progress?.invoke(done + 1, total, "manual: tap kirim")
                done++
                Handler(Looper.getMainLooper()).postDelayed({ fireNext() }, delayMs)
            }
        }

        internal fun onSendClicked() {
            if (!awaitingSend) return
            awaitingSend = false
            done++
            Handler(Looper.getMainLooper()).postDelayed({ fireNext() }, delayMs)
        }
    }

    override fun onServiceConnected() { super.onServiceConnected(); instance = this }
    override fun onDestroy() { if (instance == this) instance = null; super.onDestroy() }
    override fun onInterrupt() {}

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        if (!awaitingSend) return
        val root = rootInActiveWindow ?: return
        val pkg = root.packageName?.toString() ?: return
        if (pkg != "com.whatsapp" && pkg != "com.whatsapp.w4b") return
        val btn = findSend(root) ?: return
        if (btn.performAction(AccessibilityNodeInfo.ACTION_CLICK)) onSendClicked()
    }

    private fun findSend(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        for (id in listOf("com.whatsapp:id/send", "com.whatsapp.w4b:id/send",
                          "com.whatsapp:id/send_button", "com.whatsapp.w4b:id/send_button")) {
            root.findAccessibilityNodeInfosByViewId(id).firstOrNull { it.isEnabled }?.let { return it }
        }
        for (d in listOf("Send", "Kirim", "Enviar", "Envoyer")) {
            root.findAccessibilityNodeInfosByText(d).firstOrNull {
                it.isClickable || it.parent?.isClickable == true
            }?.let { return it }
        }
        return null
    }
}
