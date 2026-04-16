package com.example.biometricos.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TrainingSession(
    val id: String? = null,
    val username: String,
    val rawText: String,
    val distanceKm: Double,
    val durationMin: Double,
    val timestamp: Long
)

@Serializable
data class BiometricLog(
    val id: String? = null,
    val username: String,
    val success: Boolean,
    val timestamp: Long
)

class AthleteApi {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    /**
     * URL de la Nube (Render)
     * Reemplaza 'tu-app-biometricos' por el nombre real que le pusiste en Render.
     */
    private val baseUrl = "https://biometricos2.onrender.com"

    suspend fun saveTraining(session: TrainingSession): Boolean {
        return try {
            val response = client.post("$baseUrl/trainings") {
                setBody(session)
                header("Content-Type", "application/json")
            }
            response.status.value in 200..299
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getTrainings(username: String): List<TrainingSession> {
        return try {
            client.get("$baseUrl/trainings/$username").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveBiometricLog(log: BiometricLog): Boolean {
        return try {
            val response = client.post("$baseUrl/biometric-logs") {
                setBody(log)
                header("Content-Type", "application/json")
            }
            response.status.value in 200..299
        } catch (e: Exception) {
            false
        }
    }
}
