@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.example.biometricos

import platform.UIKit.UIDevice
import platform.LocalAuthentication.*
import platform.Foundation.*
import kotlinx.cinterop.*
import platform.Speech.*
import platform.AVFoundation.*
import platform.AVFAudio.*
import platform.Network.*

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    
    private val speechRecognizer = SFSpeechRecognizer(NSLocale(localeIdentifier = "es-MX"))
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest? = null
    private var recognitionTask: SFSpeechRecognitionTask? = null
    private val audioEngine = AVAudioEngine()

    override fun authenticate(onResult: (Boolean) -> Unit) {
        val context = LAContext()
        context.evaluatePolicy(LAPolicyDeviceOwnerAuthentication, "Acceso Biométrico") { success, _ ->
            onResult(success)
        }
    }

    override fun startListening(onResult: (String) -> Unit) {
        // RF02 - Captura por voz real en iOS
        SFSpeechRecognizer.requestAuthorization { status: SFSpeechRecognizerAuthorizationStatus ->
            if (status == SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized) {
                try {
                    // Detener cualquier sesión previa
                    if (audioEngine.running) {
                        audioEngine.stop()
                        recognitionRequest?.endAudio()
                    }

                    recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
                    val inputNode = audioEngine.inputNode
                    val recordingFormat = inputNode.outputFormatForBus(0u)
                    
                    inputNode.removeTapOnBus(0u)
                    inputNode.installTapOnBus(0u, 1024u, recordingFormat) { buffer: AVAudioPCMBuffer?, _ ->
                        recognitionRequest?.appendAudioPCMBuffer(buffer!!)
                    }
                    
                    audioEngine.prepare()
                    val errorPtr = nativeHeap.alloc<ObjCObjectVar<NSError?>>()
                    if (audioEngine.startAndReturnError(errorPtr.ptr)) {
                        recognitionTask = speechRecognizer?.recognitionTaskWithRequest(recognitionRequest!!) { result: SFSpeechRecognitionResult?, error: NSError? ->
                            if (result != null) {
                                onResult(result.bestTranscription.formattedString)
                            }
                            if (error != null) {
                                stopListening()
                            }
                        }
                    } else {
                        onResult("ERROR: No se pudo iniciar el motor de audio")
                    }
                } catch (e: Exception) {
                    onResult("ERROR: ${e.message}")
                }
            } else {
                onResult("ERROR: Permiso de micrófono denegado")
            }
        }
    }

    override fun stopListening() {
        if (audioEngine.running) {
            audioEngine.stop()
            audioEngine.inputNode.removeTapOnBus(0u)
            recognitionRequest?.endAudio()
            recognitionTask?.cancel()
        }
    }

    override fun isNetworkAvailable(): Boolean = true
}

actual fun getPlatform(): Platform = IOSPlatform()
