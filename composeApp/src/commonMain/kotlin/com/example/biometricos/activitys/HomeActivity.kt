package com.example.biometricos.activitys

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biometricos.getPlatform
import com.example.biometricos.data.TrainingRepository
import com.example.biometricos.network.TrainingSession
import kotlinx.coroutines.launch
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.FloatLinearAxisModel
import io.github.koalaplot.core.xygraph.DefaultPoint
import io.github.koalaplot.core.line.LinePlot
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import kotlinx.datetime.Clock as KtClock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeActivity(
    userName: String, 
    repository: TrainingRepository,
    onBackToLogin: () -> Unit
) {
    val platform = getPlatform()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    
    // Colores del Login
    val darkGold = Color(0xFFC5A358)
    val darkBackground = Color(0xFF1A1C1E)
    val lightGray = Color(0xFF8E8E8E)
    val cardBackground = Color(0xFF25282B)

    var isRecording by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var transcribedText by remember { mutableStateOf("") }
    var extractedKm by remember { mutableStateOf(0.0) }
    var extractedMin by remember { mutableStateOf(0.0) }
    var history by remember { mutableStateOf<List<TrainingSession>>(emptyList()) }

    // Función para extraer números del texto (Distancia y Tiempo)
    fun processTranscription(text: String) {
        val kmRegex = "(\\d+([.,]\\d+)?)\\s*(km|kilómetros|kilometros|kilómetro)".toRegex(RegexOption.IGNORE_CASE)
        val minRegex = "(\\d+([.,]\\d+)?)\\s*(min|minutos|minuto)".toRegex(RegexOption.IGNORE_CASE)
        
        extractedKm = kmRegex.find(text)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        extractedMin = minRegex.find(text)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
    }

    // Guardar usando el repositorio (Local -> Nube)
    suspend fun saveProgress(text: String, km: Double, min: Double) {
        if (isSaving) return
        isSaving = true
        try {
            val success = repository.saveTraining(userName, text, km, min)
            if (success) {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar("Guardado localmente. Sincronizando...")
                history = repository.getTrainings(userName)
                transcribedText = ""
                extractedKm = 0.0
                extractedMin = 0.0
            }
        } finally {
            isSaving = false
        }
    }

    LaunchedEffect(Unit) {
        history = repository.getTrainings(userName)
    }

    Scaffold(
        containerColor = darkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "BIENVENIDO", 
                            fontWeight = FontWeight.ExtraLight, 
                            color = Color.White,
                            fontSize = 14.sp,
                            letterSpacing = 4.sp
                        )
                        Text(
                            userName.uppercase(), 
                            fontWeight = FontWeight.Bold, 
                            color = darkGold,
                            fontSize = 18.sp,
                            letterSpacing = 2.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackToLogin) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Cerrar sesión",
                            tint = darkGold
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = darkBackground,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "DICTAR ENTRENAMIENTO", 
                        style = MaterialTheme.typography.titleMedium,
                        color = darkGold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { 
                                if (!isRecording) {
                                    isRecording = true
                                    platform.startListening { result ->
                                        if (result.startsWith("ERROR:")) {
                                            scope.launch { 
                                                snackbarHostState.currentSnackbarData?.dismiss()
                                                snackbarHostState.showSnackbar(result) 
                                            }
                                            isRecording = false
                                        } else {
                                            transcribedText = result
                                            processTranscription(result)
                                        }
                                    }
                                } else {
                                    isRecording = false
                                    platform.stopListening()
                                    if (transcribedText.isNotBlank() && (extractedKm > 0 || extractedMin > 0)) {
                                        scope.launch {
                                            saveProgress(transcribedText, extractedKm, extractedMin)
                                        }
                                    }
                                }
                            },
                            shape = CircleShape,
                            modifier = Modifier.size(80.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecording) Color.Red else darkGold
                            )
                        ) {
                            Icon(
                                if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = if (isRecording) Color.White else darkBackground
                            )
                        }
                    }
                    
                    if (isRecording) {
                        Text(
                            "ESCUCHANDO...", 
                            color = Color.Red, 
                            modifier = Modifier.padding(top = 12.dp),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    OutlinedTextField(
                        value = transcribedText,
                        onValueChange = { 
                            transcribedText = it 
                            processTranscription(it)
                        },
                        label = { Text("Nota de avances", color = lightGray) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Hable para dictar sus avances...", color = lightGray.copy(alpha = 0.5f)) },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                focusManager.clearFocus()
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = darkGold,
                            unfocusedBorderColor = lightGray,
                            focusedLabelColor = darkGold
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("DISTANCIA", color = lightGray, fontSize = 10.sp, letterSpacing = 1.sp)
                            Text("$extractedKm km", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("TIEMPO", color = lightGray, fontSize = 10.sp, letterSpacing = 1.sp)
                            Text("${extractedMin.toInt()} min", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            scope.launch {
                                saveProgress(transcribedText, extractedKm, extractedMin)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = darkGold),
                        enabled = !isSaving && transcribedText.isNotBlank() && (extractedKm > 0 || extractedMin > 0)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = darkBackground,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = darkBackground)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("GUARDAR AVANCES", color = darkBackground, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "PROGRESO ESTADÍSTICO", 
                style = MaterialTheme.typography.titleMedium,
                color = darkGold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (history.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .background(cardBackground)
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    TrainingChart(history, darkGold)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    "HISTORIAL RECIENTE", 
                    color = lightGray, 
                    fontSize = 12.sp, 
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                )
                
                history.forEach { session ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBackground)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${session.distanceKm} KM", 
                                    fontWeight = FontWeight.Bold,
                                    color = darkGold
                                )
                                Text(
                                    "${session.durationMin.toInt()} MIN", 
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                session.rawText, 
                                style = MaterialTheme.typography.bodySmall, 
                                color = lightGray
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No hay registros previos", color = lightGray)
                    Text("¡Dicta tu primer entrenamiento!", color = lightGray, fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun TrainingChart(history: List<TrainingSession>, lineColor: Color) {
    val sortedHistory = history.sortedBy { it.timestamp }
    val data = sortedHistory.mapIndexed { index, session -> 
        DefaultPoint(index.toFloat(), session.distanceKm.toFloat()) 
    }
    
    val xMax = (history.size.toFloat() - 1f).coerceAtLeast(1f)
    val yMax = (history.maxOfOrNull { it.distanceKm }?.toFloat() ?: 5f) + 2f

    XYGraph(
        xAxisModel = FloatLinearAxisModel(0f..xMax),
        yAxisModel = FloatLinearAxisModel(0f..yMax),
        modifier = Modifier.fillMaxWidth().height(220.dp),
        xAxisLabels = { x: Float -> (x.toInt() + 1).toString() },
        yAxisLabels = { y: Float -> y.toInt().toString() },
        xAxisTitle = "Sesiones Realizadas",
        yAxisTitle = "Distancia Recorrida (Km)"
    ) {
        LinePlot(
            data = data,
            lineStyle = LineStyle(brush = SolidColor(lineColor), strokeWidth = 3.dp)
        )
    }
}
