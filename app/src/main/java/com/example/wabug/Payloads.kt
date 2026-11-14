package com.example.wabug
object Payloads {
    data class P(val id: String, val label: String, val gen: () -> String)
    val ALL = listOf(
        P("emoji_flood", "emoji flood 6000", { "\uD83D\uDE00".repeat(6000) }),
        P("large_draft", "large draft 85K", { "A".repeat(85000) }),
        P("blank", "blank ZWSP", { "\u200B".repeat(5000) })
    )
    fun get(id: String) = ALL.firstOrNull { it.id == id }
}
