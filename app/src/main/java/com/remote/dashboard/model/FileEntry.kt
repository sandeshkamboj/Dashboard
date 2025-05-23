package com.remote.dashboard.model

import kotlinx.serialization.Serializable

@Serializable
data class FileEntry(
    val name: String,
    val path: String,
    val size: Long,
    val type: String,
    val lastModified: Long,
)