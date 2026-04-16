package com.example.biometricos

import androidx.compose.ui.window.ComposeUIViewController
import com.example.biometricos.db.DatabaseDriverFactory

fun MainViewController() = ComposeUIViewController { 
    val databaseDriverFactory = DatabaseDriverFactory()
    App(databaseDriverFactory) 
}