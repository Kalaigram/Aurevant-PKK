package com.aurevant.pkk.core.network

/** Kontrak API: {status, message, data, errors}. */
data class Envelope<T>(
    val status: String?,
    val message: String?,
    val data: T?,
    val errors: Map<String, List<String>>?,
)

class ApiError(
    val code: Int,
    override val message: String,
    val errors: Map<String, List<String>>? = null,
) : Exception(message)
