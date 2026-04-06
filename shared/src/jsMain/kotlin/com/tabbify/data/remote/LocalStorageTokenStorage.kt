package com.tabbify.data.remote

class LocalStorageTokenStorage : TokenStorage {

    override fun getToken(): String? =
        js("localStorage.getItem('tabbify_jwt')") as? String

    override fun setToken(token: String?) {
        if (token != null) js("localStorage.setItem('tabbify_jwt', token)")
        else js("localStorage.removeItem('tabbify_jwt')")
    }

    override fun getUserId(): String? =
        js("localStorage.getItem('tabbify_user_id')") as? String

    override fun setUserId(id: String?) {
        if (id != null) js("localStorage.setItem('tabbify_user_id', id)")
        else js("localStorage.removeItem('tabbify_user_id')")
    }

    override fun clear() {
        js("localStorage.removeItem('tabbify_jwt'); localStorage.removeItem('tabbify_user_id')")
    }
}
