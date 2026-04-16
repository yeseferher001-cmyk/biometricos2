package com.example.biometricos

import androidx.compose.runtime.*
import com.example.biometricos.activitys.HomeActivity
import com.example.biometricos.activitys.LoginActivity
import com.example.biometricos.db.AppDatabase
import com.example.biometricos.db.DatabaseDriverFactory

@Composable
fun App(databaseDriverFactory: DatabaseDriverFactory) {
    val database = remember { AppDatabase(databaseDriverFactory.createDriver()) }
    var isAuthenticated by remember { mutableStateOf(false) }
    var lastUser by remember { mutableStateOf("") }

    if (isAuthenticated) {
        HomeActivity(
            userName = lastUser,
            onBackToLogin = {
                isAuthenticated = false
            }
        )
    } else {
        LoginActivity(
            onSaveUser = { name ->
                database.appDatabaseQueries.insertUser(name)
            },
            autenticacionExitosa = {
                lastUser = database.appDatabaseQueries.getLastUser().executeAsOneOrNull()?.username ?: "Usuario"
                isAuthenticated = true
            }
        )
    }
}
