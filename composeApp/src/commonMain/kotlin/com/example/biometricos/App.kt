package com.example.biometricos

import androidx.compose.runtime.*
import com.example.biometricos.activitys.HomeActivity
import com.example.biometricos.activitys.LoginActivity
import com.example.biometricos.db.AppDatabase
import com.example.biometricos.db.DatabaseDriverFactory
import com.example.biometricos.data.TrainingRepository
import com.example.biometricos.network.AthleteApi

@Composable
fun App(databaseDriverFactory: DatabaseDriverFactory) {
    val platform = getPlatform()
    val database = remember { AppDatabase(databaseDriverFactory.createDriver()) }
    val api = remember { AthleteApi() }
    val repository = remember { TrainingRepository(database, api, platform) }
    
    var isAuthenticated by remember { mutableStateOf(false) }
    var lastUser by remember { mutableStateOf("") }

    if (isAuthenticated) {
        HomeActivity(
            userName = lastUser,
            repository = repository,
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
