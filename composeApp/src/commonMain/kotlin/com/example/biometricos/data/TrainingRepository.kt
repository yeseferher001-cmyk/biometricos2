package com.example.biometricos.data

import com.example.biometricos.db.AppDatabase
import com.example.biometricos.network.AthleteApi
import com.example.biometricos.network.TrainingSession
import com.example.biometricos.Platform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.random.Random

class TrainingRepository(
    private val database: AppDatabase,
    private val api: AthleteApi,
    private val platform: Platform
) {
    private val queries = database.appDatabaseQueries

    suspend fun saveTraining(
        username: String,
        text: String,
        km: Double,
        min: Double
    ): Boolean = withContext(Dispatchers.IO) {
        val id = "local_" + Clock.System.now().toEpochMilliseconds() + "_" + Random.nextInt(1000)
        val timestamp = Clock.System.now().toEpochMilliseconds()
        
        try {
            // 1. Guardar localmente
            queries.insertTraining(
                id = id,
                username = username,
                rawText = text,
                distanceKm = km,
                durationMin = min,
                timestamp = timestamp,
                synced = 0L
            )

            // 2. Intentar subir a MongoDB si hay red
            if (platform.isNetworkAvailable()) {
                val session = TrainingSession(
                    id = null,
                    username = username,
                    rawText = text,
                    distanceKm = km,
                    durationMin = min,
                    timestamp = timestamp
                )
                if (api.saveTraining(session)) {
                    queries.markAsSynced(id)
                }
            }
        } catch (e: Exception) {
            println("Error saving training: ${e.message}")
            return@withContext false
        }
        
        return@withContext true
    }

    suspend fun getTrainings(username: String): List<TrainingSession> = withContext(Dispatchers.IO) {
        try {
            if (platform.isNetworkAvailable()) {
                syncPendingTrainings()
                val remote = api.getTrainings(username)
                if (remote.isNotEmpty()) {
                    remote.forEach { session ->
                        queries.insertTraining(
                            id = session.id ?: Random.nextInt().toString(),
                            username = session.username,
                            rawText = session.rawText,
                            distanceKm = session.distanceKm,
                            durationMin = session.durationMin,
                            timestamp = session.timestamp,
                            synced = 1L
                        )
                    }
                }
            }

            return@withContext queries.getAllTrainings(username).executeAsList().map {
                TrainingSession(
                    id = it.id,
                    username = it.username,
                    rawText = it.rawText,
                    distanceKm = it.distanceKm,
                    durationMin = it.durationMin,
                    timestamp = it.timestamp
                )
            }
        } catch (e: Exception) {
            println("Error fetching trainings: ${e.message}")
            return@withContext emptyList<TrainingSession>()
        }
    }

    suspend fun syncPendingTrainings() {
        if (!platform.isNetworkAvailable()) return

        try {
            val pending = queries.getUnsyncedTrainings().executeAsList()
            for (local in pending) {
                val session = TrainingSession(
                    id = null,
                    username = local.username,
                    rawText = local.rawText,
                    distanceKm = local.distanceKm,
                    durationMin = local.durationMin,
                    timestamp = local.timestamp
                )
                if (api.saveTraining(session)) {
                    queries.markAsSynced(local.id)
                }
            }
        } catch (e: Exception) {
            println("Sync failed: ${e.message}")
        }
    }
}
