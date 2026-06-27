package com.iicytower.wanderlist.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SecureKeyStorage(context: Context) {

    private val sharedPreferences = EncryptedSharedPreferences.create(
        "secure_prefs",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveKey(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    fun getKey(key: String): String =
        sharedPreferences.getString(key, "") ?: ""

    fun clearKey(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }
}
