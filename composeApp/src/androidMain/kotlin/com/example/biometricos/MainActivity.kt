package com.example.biometricos

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.example.biometricos.db.DatabaseDriverFactory

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        currentActivity = this
        val databaseDriverFactory = DatabaseDriverFactory()
        setContent {
            App(databaseDriverFactory)
        }
    }
}
