package com.tabbify.data.remote

actual fun createTokenStorage(): TokenStorage = SharedPrefsTokenStorage()
