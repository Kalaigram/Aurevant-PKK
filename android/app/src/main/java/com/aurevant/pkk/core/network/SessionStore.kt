package com.aurevant.pkk.core.network

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.sessionStore by preferencesDataStore("sesi")

/** Token + perusahaan aktif. Satu-satunya sumber sesi di aplikasi. */
@Singleton
class SessionStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val tokenKey = stringPreferencesKey("token")
    private val userIdKey = intPreferencesKey("user_id")
    private val userNameKey = stringPreferencesKey("user_name")
    private val companyKey = intPreferencesKey("company_id")
    private val companyNameKey = stringPreferencesKey("company_name")

    val token: Flow<String?> = context.sessionStore.data.map { it[tokenKey] }
    val companyId: Flow<Int?> = context.sessionStore.data.map { it[companyKey] }
    val userId: Flow<Int?> = context.sessionStore.data.map { it[userIdKey] }

    fun tokenBlocking(): String? = runBlocking { token.first() }

    suspend fun saveLogin(token: String, userId: Int, userName: String) {
        context.sessionStore.edit {
            it[tokenKey] = token
            it[userIdKey] = userId
            it[userNameKey] = userName
        }
    }

    suspend fun saveCompany(id: Int, name: String) {
        context.sessionStore.edit {
            it[companyKey] = id
            it[companyNameKey] = name
        }
    }

    suspend fun clear() {
        context.sessionStore.edit { it.clear() }
    }
}
