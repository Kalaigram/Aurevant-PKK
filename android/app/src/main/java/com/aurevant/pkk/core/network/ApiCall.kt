package com.aurevant.pkk.core.network

import retrofit2.Response

/** Ambil data dari amplop sukses; lempar ApiError bila kosong. Galat HTTP sudah jadi ApiError di interceptor. */
fun <T> unwrap(response: Response<Envelope<T>>): T {
    val body = response.body()
    return body?.data ?: throw ApiError(
        response.code(),
        body?.message ?: "Respons kosong dari server.",
        body?.errors,
    )
}
