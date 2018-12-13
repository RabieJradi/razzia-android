package jradi.rabie.dk.razzia_android.model

import android.content.Context
import android.preference.PreferenceManager


interface CacheProviderInterface {
    suspend fun saveValue(value: String)
    suspend fun getStringValue(): String?

    suspend fun saveValue(value: Boolean)
    suspend fun getBooleanValue(): Boolean

}

enum class CacheKey(val value: String) {
    ActivityRecognitionCache("CacheKey.ActivityRecognitionCache.requestState"),
    UserLocationPermissionCache("CacheKey.UserLocationPermissionCache.permissionState")
}


class CacheProvider(private val cacheKey: CacheKey, private val context: Context) : CacheProviderInterface {
    private val pref = PreferenceManager.getDefaultSharedPreferences(context)

    override suspend fun saveValue(value: String) {
        pref.edit().putString(cacheKey.value, value).apply()
    }

    override suspend fun getStringValue(): String? {
        return pref.getString(cacheKey.value, null)
    }

    override suspend fun saveValue(value: Boolean) {
        pref.edit().putBoolean(cacheKey.value, value).apply()
    }

    override suspend fun getBooleanValue(): Boolean {
        return pref.getBoolean(cacheKey.value, false)
    }
}