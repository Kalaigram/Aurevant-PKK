package com.aurevant.pkk.core.push

import android.util.Log
import com.aurevant.pkk.core.network.ApiService
import com.aurevant.pkk.core.network.ApiError
import com.aurevant.pkk.core.network.SessionStore
import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Daftarkan token FCM perangkat ke server setiap kali sesi/perusahaan siap.
 * Aman dipanggil walau google-services.json belum dipasang (gagal diam-diam).
 */
@Singleton
class PushTokenSync @Inject constructor(
    private val api: ApiService,
    private val session: SessionStore,
) {
    suspend fun sync() {
        if (session.tokenBlocking() == null) return
        val token = fcmToken() ?: return
        try {
            val response = api.registerToken(mapOf("token" to token, "platform" to "android"))
            if (!response.isSuccessful) {
                Log.w(TAG, "Gagal menyimpan token perangkat (HTTP ${response.code()}).")
            }
        } catch (e: ApiError) {
            Log.w(TAG, "Gagal menyimpan token perangkat: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Gagal menyimpan token perangkat: ${e.message}")
        }
    }

    private suspend fun fcmToken(): String? = try {
        suspendCancellableCoroutine { cont ->
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (cont.isActive) {
                        cont.resume(if (task.isSuccessful) task.result else null)
                    }
                }
        }
    } catch (e: Exception) {
        Log.i(TAG, "FCM belum dikonfigurasi: ${e.message}")
        null
    }

    private companion object {
        const val TAG = "PushTokenSync"
    }
}
