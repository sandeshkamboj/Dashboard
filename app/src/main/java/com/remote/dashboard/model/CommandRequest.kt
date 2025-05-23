package com.remote.dashboard.model

data class CommandRequest(
    val deviceId: String,
    val type: String,
    val options: Map<String, Any>? = null
)