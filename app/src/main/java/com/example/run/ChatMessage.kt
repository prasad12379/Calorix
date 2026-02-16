package com.example.run

data class ChatMessage(
    val message: String,
    val isBot: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)