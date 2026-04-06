package com.tabbify.data.remote

interface TokenStorage {
    fun getToken(): String?
    fun setToken(token: String?)
    fun getUserId(): String?
    fun setUserId(id: String?)
    fun clear()
}
