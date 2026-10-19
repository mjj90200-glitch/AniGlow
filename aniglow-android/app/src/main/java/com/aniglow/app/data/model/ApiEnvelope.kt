package com.aniglow.app.data.model

data class ApiEnvelope<T>(
    val success: Boolean = true,
    val code: Int = 0,
    val message: String? = null,
    val data: T? = null,
)
