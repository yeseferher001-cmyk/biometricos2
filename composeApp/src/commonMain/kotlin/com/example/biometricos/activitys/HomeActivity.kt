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

    // Función para extraer números del texto (Distancia y Tiempo) - RF03
    fun processTranscription(text: String) {
        val kmRegex = "(\\d+([.,]\\d+)?)\\s*(km|kilómetros|kilometros|kilómetro)".toRegex(RegexOption.IGNORE_CASE)
        val minRegex = "(\\d+([.,]\\d+)?)\\s*(min|minutos|minuto)".toRegex(RegexOption.IGNORE_CASE)
        
        extractedKm = kmRegex.find(text)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        extractedMin = minRegex.find(text)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
    }

    LaunchedEffect(Unit) {
        if (platform.isNetworkAvailable()) {
            history = api.getTrainings(userName)
        } else {
            // RNF04 - Disponibilidad
            scope.launch { snackbarHostState.showSnackbar("Sin conexión a internet") }
        }
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
            // RF02 - Captura por Voz
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
                        // RNF02 - Usabilidad (Botones grandes)
                        Button(
                            onClick = { 
                                if (!isRecording) {
                                    isRecording = true
                                    platform.startListening { result ->
                                        isRecording = false // Detener estado visual al recibir resultado
                                        if (result.startsWith("ERROR:")) {
                                            scope.launch { snackbarHostState.showSnackbar(result) }
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
                    
                    // RF03 - Confirmar o editar texto
                    OutlinedTextField(
                        value = transcribedText,
                        onValueChange = { 
                            transcribedText = it 
                            processTranscription(it)
                        },
                        label = { Text("Nota de avances", color = lightGray) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Hable para dictar sus avances...", color = lightGray.copy(alpha = 0.5f)) },
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
                    
                    // RF04 - Persistencia de Datos
                    Button(
                        onClick = {
                            scope.launch {
                                if (!platform.isNetworkAvailable()) {
                                    snackbarHostState.showSnackbar("No hay conexión a internet")
                                    return@launch
                                }
                                
                                val session = TrainingSession(
                                    username = userName,
                                    rawText = transcribedText,
                                    distanceKm = extractedKm,
                                    durationMin = extractedMin,
                                    timestamp = KtClock.System.now().toEpochMilliseconds()
                                )
                                val success = api.saveTraining(session)
                                if (success) {
                                    snackbarHostState.showSnackbar("Avances guardados exitosamente")
                                    history = api.getTrainings(userName)
                                    transcribedText = ""
                                    extractedKm = 0.0
                                    extractedMin = 0.0
                                } else {
                                    snackbarHostState.showSnackbar("Error al guardar en la nube (Verifique Render)")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = darkGold),
                        enabled = transcribedText.isNotBlank() && (extractedKm > 0 || extractedMin > 0)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = darkBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SUBIR AVANCES", color = darkBackground, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            // RF05 - Visualización de Progreso
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
    // Ordenar por fecha para que la gráfica tenga sentido - RF05
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
