package com.example.biometricos.activitys

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biometricos.getPlatform

@Composable
fun LoginActivity(
    onSaveUser: (String) -> Unit,
    autenticacionExitosa: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    val darkGold = Color(0xFFC5A358)
    val darkBackground = Color(0xFF1A1C1E)
    val lightGray = Color(0xFF8E8E8E)
    val platform = getPlatform()
    
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Spacer(modifier = Modifier.height(100.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "BIENVENIDO",
                    color = Color.White,
                    fontSize = 32.sp,
                    letterSpacing = 6.sp,
                    fontWeight = FontWeight.ExtraLight
                )
                Text(
                    text = "A LA BITACORA",
                    color = darkGold,
                    fontSize = 16.sp,
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(80.dp))

            // Username Field
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = lightGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (username.isEmpty()) {
                            Text("Bienvenido", color = lightGray, fontSize = 16.sp)
                        }
                        BasicTextField(
                            value = username,
                            onValueChange = { username = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                            )
                        )
                    }
                }
                HorizontalDivider(
                    color = lightGray.copy(alpha = 0.5f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Central Biometric Buttons
            Text(
                text = "Seleccione su método de acceso",
                color = lightGray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(40.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 100.dp)
            ) {
                // Botón de Huella
                IconButton(
                    onClick = {
                        if (username.isNotBlank()) {
                            onSaveUser(username)
                            platform.authenticate { success ->
                                if (success) {
                                    autenticacionExitosa()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(80.dp)
                        .border(1.dp, darkGold.copy(alpha = 0.5f), CircleShape)
                        .padding(4.dp)
                        .border(1.dp, darkGold, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Huella",
                        tint = darkGold,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Botón de Rostro
                IconButton(
                    onClick = {
                        if (username.isNotBlank()) {
                            onSaveUser(username)
                            platform.authenticate { success ->
                                if (success) {
                                    autenticacionExitosa()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(80.dp)
                        .border(1.dp, darkGold.copy(alpha = 0.5f), CircleShape)
                        .padding(4.dp)
                        .border(1.dp, darkGold, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "Rostro",
                        tint = darkGold,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
