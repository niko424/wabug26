package com.example.wabug

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.wabug.WAAutoService.Companion.Job

class MainActivity : AppCompatActivity() {

    private lateinit var pagePrivate: LinearLayout
    private lateinit var pageGlobal: LinearLayout
    private lateinit var pageSettings: LinearLayout

    private lateinit var numIn: EditText
    private lateinit var globalIn: EditText
    private lateinit var payloadSpinner: Spinner
    private lateinit var pkgSpinner: Spinner
    private lateinit var delayIn: EditText
    private lateinit var statusTv: TextView
    private lateinit var a11yStatus: TextView

    private lateinit var payloadNames: List<String>
    private lateinit var payloadIds: List<String>

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
        }
        setContentView(root)

        payloadNames = Payloads.ALL.map { it.label }
        payloadIds = Payloads.ALL.map { it.id }

        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (t in listOf("Privat", "Global", "Setelan")) {
            tabs.addView(Button(this).apply {
                text = t
                setOnClickListener { switchTo(t) }
            }, LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(tabs)

        pagePrivate = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        pagePrivate.addView(TextView(this).apply { text = "nomor target (62xxx)" })
        numIn = EditText(this).apply {
            hint = "628123456789"
            inputType = InputType.TYPE_CLASS_PHONE; singleLine = true
            setText(Config.lastNumber(this@MainActivity))
        }
        pagePrivate.addView(numIn)
        pagePrivate.addView(TextView(this).apply { text = "payload" })
        payloadSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, payloadNames)
            setSelection(payloadIds.indexOf(Config.lastPayload(this@MainActivity)).coerceAtLeast(0))
        }
        pagePrivate.addView(payloadSpinner)
        pagePrivate.addView(Button(this).apply {
            text = "KIRIM"
            setOnClickListener {
                val n = numIn.text.toString().trim()
                if (n.isEmpty()) { status("isi nomor"); return@setOnClickListener }
                Config.saveLastNumber(this@MainActivity, n)
                Config.saveLastPayload(this@MainActivity, payloadIds[payloadSpinner.selectedItemPosition])
                fireQueue(listOf(normalize(n)))
            }
        })
        root.addView(pagePrivate)

        pageGlobal = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        pageGlobal.addView(TextView(this).apply { text = "list nomor (baris/koma)" })
        globalIn = EditText(this).apply {
            hint = "628111...\n628222..."
            minLines = 6; gravity = android.view.Gravity.TOP
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        pageGlobal.addView(globalIn)
        pageGlobal.addView(Button(this).apply {
            text = "BROADCAST"
            setOnClickListener {
                val nums = parseNumbers(globalIn.text.toString())
                if (nums.isEmpty()) { status("list kosong"); return@setOnClickListener }
                status("queue: ${nums.size}")
                fireQueue(nums)
            }
        })
        root.addView(pageGlobal)

        pageSettings = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        pageSettings.addView(TextView(this).apply { text = "paket WhatsApp" })
        pkgSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                listOf("WhatsApp", "WhatsApp Business", "keduanya"))
            setSelection(Config.pkgIdx(this@MainActivity))
        }
        pageSettings.addView(pkgSpinner)
        pageSettings.addView(TextView(this).apply { text = "delay antar pesan (ms)" })
        delayIn = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(Config.delayMs(this@MainActivity).toString())
        }
        pageSettings.addView(delayIn)
        pageSettings.addView(Button(this).apply {
            text = "Simpan"
            setOnClickListener {
                Config.saveDelay(this@MainActivity, delayIn.text.toString().toLongOrNull() ?: 3000L)
                Config.savePkgIdx(this@MainActivity, pkgSpinner.selectedItemPosition)
                WAAutoService.delayMs = Config.delayMs(this@MainActivity)
                status("delay=${WAAutoService.delayMs}ms")
            }
        })
        val autoCheck = CheckBox(this).apply {
            text = "auto-tap kirim"
            isChecked = Config.autoSend(this@MainActivity)
            setOnCheckedChangeListener { _, v -> Config.saveAutoSend(this@MainActivity, v) }
        }
        pageSettings.addView(autoCheck)
        pageSettings.addView(Button(this).apply {
            text = "Buka Aksesibilitas"
            setOnClickListener {
                try { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
            }
        })
        a11yStatus = TextView(this).apply { textSize = 11f; setPadding(0, 20, 0, 0) }
        pageSettings.addView(a11yStatus)
        root.addView(pageSettings)

        statusTv = TextView(this).apply { textSize = 11f; setPadding(0, 30, 0, 0) }
        root.addView(statusTv)

        switchTo("Privat")
        refreshA11y()
    }

    override fun onResume() { super.onResume(); refreshA11y() }

    private fun refreshA11y() {
        a11yStatus.text = if (WAAutoService.instance != null) "aksesibilitas: AKTIF" else "aksesibilitas: MATI"
    }

    private fun switchTo(name: String) {
        pagePrivate.visibility = if (name == "Privat") View.VISIBLE else View.GONE
        pageGlobal.visibility = if (name == "Global") View.VISIBLE else View.GONE
        pageSettings.visibility = if (name == "Setelan") View.VISIBLE else View.GONE
    }

    private fun parseNumbers(raw: String): List<String> =
        raw.split("\n", ",", ";", " ", "\t")
            .map { it.trim() }.filter { it.isNotEmpty() }
            .map { normalize(it) }.filter { it.isNotEmpty() }.distinct()

    private fun normalize(n: String): String {
        var s = n.replace(Regex("[^0-9+]"), "")
        if (s.startsWith("+")) s = s.substring(1)
        if (s.startsWith("0")) s = "62" + s.substring(1)
        return s
    }

    private fun fireQueue(numbers: List<String>) {
        val payloadId = payloadIds[payloadSpinner.selectedItemPosition]
        val pkgs = when (pkgSpinner.selectedItemPosition) {
            0 -> listOf("com.whatsapp")
            1 -> listOf("com.whatsapp.w4b")
            else -> listOf("com.whatsapp", "com.whatsapp.w4b")
        }
        val jobs = mutableListOf<Job>()
        for (n in numbers) for (p in pkgs) jobs.add(Job(n, payloadId, p))
        WAAutoService.delayMs = Config.delayMs(this)
        WAAutoService.enqueue(jobs, Config.autoSend(this)) { i, t, info ->
            runOnUiThread { statusTv.text = "[$i/$t] $info" }
        }
        if (Config.autoSend(this) && WAAutoService.instance == null) {
            status("aksesibilitas mati — manual tap")
        }
    }

    private fun status(s: String) { statusTv.text = s }
}
