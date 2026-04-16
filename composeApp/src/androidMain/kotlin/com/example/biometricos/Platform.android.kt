package com.example.biometricos

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.Locale

class AndroidPlatform(private val activity: FragmentActivity) : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"

    private var speechRecognizer: SpeechRecognizer? = null

    override fun authenticate(onResult: (Boolean) -> Unit) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e("Biometricos", "Error biometrico: $errString ($errorCode)")
                    activity.runOnUiThread {
                        Toast.makeText(activity, "Error biometrico: $errString", Toast.LENGTH_SHORT).show()
                    }
                    onResult(false)
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.d("Biometricos", "Autenticacion exitosa")
                    onResult(true)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.w("Biometricos", "Huella no reconocida")
                    activity.runOnUiThread {
                        Toast.makeText(activity, "Huella no reconocida", Toast.LENGTH_SHORT).show()
                    }
                    onResult(false)
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticación del Atleta")
            .setSubtitle("Confirma tu identidad para acceder a tu historial")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    override fun startListening(onResult: (String) -> Unit) {
        // Verificar permiso de micrófono en tiempo de ejecución
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w("Biometricos", "Permiso de microfono no otorgado. Solicitando...")
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
            onResult("ERROR: Permiso de micrófono requerido. Por favor, acéptelo e intente de nuevo.")
            return
        }

        activity.runOnUiThread {
            Log.d("Biometricos", "Iniciando reconocimiento de voz...")
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { Log.d("Biometricos", "Listo para hablar") }
                override fun onBeginningOfSpeech() { Log.d("Biometricos", "Empezó a hablar") }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { Log.d("Biometricos", "Fin del habla") }
                override fun onError(error: Int) {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Error de audio"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No se entendió el audio"
                        SpeechRecognizer.ERROR_NETWORK -> "Error de red en reconocimiento"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisos insuficientes"
                        else -> "Error $error"
                    }
                    Log.e("Biometricos", "Error SpeechRecognizer: $message")
                    onResult("ERROR: $message")
                }

                override fun onResults(results: Bundle?) {
                    val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = data?.get(0) ?: ""
                    Log.d("Biometricos", "Resultado voz: $text")
                    onResult(text)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val data = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!data.isNullOrEmpty()) {
                        Log.d("Biometricos", "Resultado parcial: ${data[0]}")
                        onResult(data[0])
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            speechRecognizer?.startListening(intent)
        }
    }

    override fun stopListening() {
        activity.runOnUiThread {
            Log.d("Biometricos", "Deteniendo reconocimiento de voz")
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }

    override fun isNetworkAvailable(): Boolean {
        val connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        val isAvailable = when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
        Log.d("Biometricos", "Red disponible: $isAvailable")
        return isAvailable
    }
}

lateinit var currentActivity: FragmentActivity
actual fun getPlatform(): Platform = AndroidPlatform(currentActivity)
