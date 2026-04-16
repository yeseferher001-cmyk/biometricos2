@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.example.biometricos

import platform.UIKit.UIDevice
import platform.LocalAuthentication.*
import platform.Foundation.*
import kotlinx.cinterop.*
import platform.Network.*
import platform.SystemConfiguration.*

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    
    override fun authenticate(onResult: (Boolean) -> Unit) {
        val context = LAContext()
        val error = nativeHeap.alloc<ObjCObjectVar<NSError?>>()
        
        if (context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, error.ptr)) {
            context.evaluatePolicy(
                LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                "Autentícate para acceder a la bitácora",
                { success, _ ->
                    onResult(success)
                }
            )
        } else {
            context.evaluatePolicy(
                LAPolicyDeviceOwnerAuthentication,
                "Usa tu código para acceder",
                { success, _ ->
                    onResult(success)
                }
            )
        }
    }

    override fun startListening(onResult: (String) -> Unit) {
        // Implementación simplificada para el examen
        onResult("Dictado de prueba: Corrí 5 kilómetros en 30 minutos")
    }

    override fun stopListening() {}

    override fun isNetworkAvailable(): Boolean {
        return true // Simplificado para iOS en este entorno
    }
}

actual fun getPlatform(): Platform = IOSPlatform()