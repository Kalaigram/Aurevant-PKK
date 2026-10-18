package com.aurevant.pkk.core.network

import com.google.gson.Gson
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

/** Sisipkan token Sanctum + ubah galat API menjadi ApiError. */
class AuthInterceptor @Inject constructor(
    private val session: SessionStore,
    private val gson: Gson,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = session.tokenBlocking()
        val request = chain.request().newBuilder()
            .addHeader("Accept", "application/json")
            .apply { if (token != null) addHeader("Authorization", "Bearer $token") }
            .build()
        val response = chain.proceed(request)
        if (response.code == 401) {
            response.close()
            throw ApiError(401, "Sesi berakhir. Silakan login lagi.")
        }
        if (!response.isSuccessful) {
            val body = response.peekBody(Long.MAX_VALUE).string()
            val envelope = runCatching {
                gson.fromJson(body, RawEnvelope::class.java)
            }.getOrNull()
            response.close()
            throw ApiError(
                response.code,
                envelope?.message ?: "HTTP ${response.code}",
                envelope?.errors,
            )
        }
        return response
    }

    private data class RawEnvelope(
        val message: String?,
        val errors: Map<String, List<String>>?,
    )
}
