package com.example.biometricos

interface Platform {
    val name: String
    fun authenticate(onResult: (Boolean) -> Unit)
    fun startListening(onResult: (String) -> Unit)
    fun stopListening()
    fun isNetworkAvailable(): Boolean
}

expect fun getPlatform(): Platform