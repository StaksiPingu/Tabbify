package com.tabbify.data.remote

import android.content.Context
import com.tabbify.platform.appContext

class SharedPrefsTokenStorage : TokenStorage {

    private val prefs by lazy {
        appContext.getSharedPreferences("tabbify_auth", Context.MODE_PRIVATE)
    }

    override fun getToken(): String? = prefs.getString("jwt_token", null)

    override fun setToken(token: String?) {
        prefs.edit().apply {
            if (token != null) putString("jwt_token", token) else remove("jwt_token")
        }.apply()
    }

    override fun getUserId(): String? = prefs.getString("user_id", null)

    override fun setUserId(id: String?) {
        prefs.edit().apply {
            if (id != null) putString("user_id", id) else remove("user_id")
        }.apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }
}
