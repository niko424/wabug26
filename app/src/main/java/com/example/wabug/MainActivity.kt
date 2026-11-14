package com.example.wabug
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.wabug.WAAutoService.Companion.Job
class MainActivity : AppCompatActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 60, 40, 40) }
        setContentView(root)
        val num = EditText(this).apply { hint = "628xxx" }
        root.addView(num)
        val btn = Button(this).apply {
            text = "KIRIM"
            setOnClickListener {
                val n = num.text.toString()
                if (n.isNotEmpty()) WAAutoService.enqueue(listOf(Job(n, "large_draft", "com.whatsapp")))
            }
        }
        root.addView(btn)
        root.addView(Button(this).apply {
            text = "Buka Aksesibilitas"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
    }
}
