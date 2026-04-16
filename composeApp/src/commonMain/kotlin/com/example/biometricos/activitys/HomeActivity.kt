package com.example.biometricos.activitys

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biometricos.getPlatform
import com.example.biometricos.network.AthleteApi
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
fun HomeActivity(userName: String, onBackToLogin: () -> Unit) {
    val platform = getPlatform()
    val api = remember { AthleteApi() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Colores del Login
    val darkGold = Color(0xFFC5A358)
    val darkBackground = Color(0xFF1A1C1E)
    val lightGray = Color(0xFF8E8E8E)
    val cardBackground = Color(0xFF25282B)

    var isRecording by remember { mutableStateOf(false) }
    var transcribedText by remember { mutableStateOf("") }
    var extractedKm by remember { mutableStateOf(0.0) }
    var extractedMin by remember { mutableStateOf(0.0) }
    var history by remember { mutableStateOf<List<TrainingSession>>(emptyList()) }

    // Función para extraer números del texto (Distancia y Tiempo)
    fun processTranscription(text: String) {
        val kmRegex = "(\\d+([.,]\\d+)?)\\s*(km|kilómetros|kilometros)".toRegex(RegexOption.IGNORE_CASE)
        val minRegex = "(\\d+([.,]\\d+)?)\\s*(min|minutos)".toRegex(RegexOption.IGNORE_CASE)
        
        extractedKm = kmRegex.find(text)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        extractedMin = minRegex.find(text)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
    }

    LaunchedEffect(Unit) {
        if (platform.isNetworkAvailable()) {
            history = api.getTrainings(userName)
        }
    }

    Scaffold(
        containerColor = darkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "MI PANEL", 
                        fontWeight = FontWeight.Light, 
                        color = Color.White,
                        letterSpacing = 4.sp
                    ) 
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
                                            scope.launch { snackbarHostState.showSnackbar(result) }
                                            isRecording = false
                                        } else {
                                            transcribedText = result
                                            processTranscription(result)
                                        }
                                    }
                                } else {
                                    isRecording = false
                                    platform.stopListening()
                                }
                            },
                            shape = CircleShape,
                            modifier = Modifier.size(70.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecording) Color.Red else darkGold
                            )
                        ) {
                            Icon(
                                if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(30.dp),
                                tint = if (isRecording) Color.White else darkBackground
                            )
                        }
                    }
                    
                    if (isRecording) {
                        Text(
                            "ESCUCHANDO...", 
                            color = Color.Red, 
                            modifier = Modifier.padding(top = 12.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (transcribedText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedTextField(
                            value = transcribedText,
                            onValueChange = { 
                                transcribedText = it 
                                processTranscription(it)
                            },
                            label = { Text("Texto Transcrito", color = lightGray) },
                            modifier = Modifier.fillMaxWidth(),
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
                            Text("Distancia: $extractedKm km", color = Color.White)
                            Text("Tiempo: $extractedMin min", color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    val session = TrainingSession(
                                        username = userName,
                                        rawText = transcribedText,
                                        distanceKm = extractedKm,
                                        durationMin = extractedMin,
                                        timestamp = KtClock.System.now().toEpochMilliseconds()
                                    )
                                    val success = api.saveTraining(session)
                                    if (success) {
                                        snackbarHostState.showSnackbar("Guardado en MongoDB")
                                        history = api.getTrainings(userName)
                                        transcribedText = ""
                                        extractedKm = 0.0
                                        extractedMin = 0.0
                                    } else {
                                        snackbarHostState.showSnackbar("Error: Verifica tu servidor en Render")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = darkGold),
                            enabled = extractedKm > 0 || extractedMin > 0
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = darkBackground)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SUBIR AVANCES", color = darkBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "HISTORIAL Y GRÁFICOS", 
                style = MaterialTheme.typography.titleMedium,
                color = darkGold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (history.isNotEmpty()) {
                Box(modifier = Modifier.background(cardBackground).padding(8.dp).fillMaxWidth()) {
                    TrainingChart(history, darkGold, Color.White)
                }
                Spacer(modifier = Modifier.height(16.dp))
                history.forEach { session ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBackground)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "${session.distanceKm} km en ${session.durationMin} min", 
                                fontWeight = FontWeight.Bold,
                                color = darkGold
                            )
                            Text(session.rawText, style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                    }
                }
            } else {
                Text("No hay entrenamientos guardados.", color = lightGray)
            }
        }
    }
}

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun TrainingChart(history: List<TrainingSession>, lineColor: Color, textColor: Color) {
    val sortedHistory = history.sortedBy { it.timestamp }
    val data = sortedHistory.mapIndexed { index, session -> 
        DefaultPoint(index.toFloat(), session.distanceKm.toFloat()) 
    }
    
    val xMax = (history.size.toFloat() - 1f).coerceAtLeast(1f)
    val yMax = (history.maxOfOrNull { it.distanceKm }?.toFloat() ?: 10f) + 2f

    XYGraph(
        xAxisModel = FloatLinearAxisModel(0f..xMax),
        yAxisModel = FloatLinearAxisModel(0f..yMax),
        modifier = Modifier.fillMaxWidth().height(200.dp),
        xAxisLabels = { x: Float -> x.toInt().toString() },
        yAxisLabels = { y: Float -> y.toInt().toString() },
        xAxisTitle = "Sesión #",
        yAxisTitle = "Kilómetros (Km)"
    ) {
        LinePlot(
            data = data,
            lineStyle = LineStyle(brush = SolidColor(lineColor), strokeWidth = 2.dp)
        )
    }
}
