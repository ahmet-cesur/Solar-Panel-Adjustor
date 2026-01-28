package com.acesur.solarpvtracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.acesur.solarpvtracker.R
import com.acesur.solarpvtracker.data.PreferencesManager
import com.acesur.solarpvtracker.ui.theme.SolarOrange
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class LanguageOption(
    val code: String,
    val name: String,
    val nativeName: String
)

val supportedLanguages = listOf(
    LanguageOption("en", "English", "English"),
    LanguageOption("es", "Spanish", "Español"),
    LanguageOption("fr", "French", "Français"),
    LanguageOption("de", "German", "Deutsch"),
    LanguageOption("pt", "Portuguese", "Português"),
    LanguageOption("tr", "Turkish", "Türkçe"),
    LanguageOption("zh", "Chinese", "中文"),
    LanguageOption("ru", "Russian", "Русский"),
    LanguageOption("bg", "Bulgarian", "Български"),
    LanguageOption("nl", "Dutch", "Nederlands"),
    LanguageOption("ar", "Arabic", "العربية"),
    LanguageOption("hi", "Hindi", "हिन्दी")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLanguageChange: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }
    
    var selectedLanguage by remember { mutableStateOf("en") }
    var darkMode by remember { mutableStateOf(false) }
    var useGps by remember { mutableStateOf(true) }
    var manualLat by remember { mutableStateOf("") }
    var manualLon by remember { mutableStateOf("") }
    
    var showLanguageDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        selectedLanguage = preferencesManager.language.first()
        darkMode = preferencesManager.darkMode.first()
        useGps = preferencesManager.useGps.first()
        manualLat = preferencesManager.manualLatitude.first().toString()
        manualLon = preferencesManager.manualLongitude.first().toString()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Language Section
            Text(
                text = stringResource(R.string.language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                onClick = { showLanguageDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        tint = SolarOrange
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = supportedLanguages.find { it.code == selectedLanguage }?.name ?: "English",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = supportedLanguages.find { it.code == selectedLanguage }?.nativeName ?: "English",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Appearance Section
            Text(
                text = stringResource(R.string.appearance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (darkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        tint = SolarOrange
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(if (darkMode) R.string.dark_mode else R.string.light_mode),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = darkMode,
                        onCheckedChange = { enabled ->
                            darkMode = enabled
                            scope.launch {
                                preferencesManager.setDarkMode(enabled)
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Location Section
            Text(
                text = stringResource(R.string.location_settings),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = SolarOrange
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.use_gps),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = useGps,
                            onCheckedChange = { enabled ->
                                useGps = enabled
                                scope.launch {
                                    preferencesManager.setUseGps(enabled)
                                }
                            }
                        )
                    }
                    
                    if (!useGps) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = manualLat,
                                onValueChange = { 
                                    manualLat = it
                                    val lat = it.toDoubleOrNull()
                                    val lon = manualLon.toDoubleOrNull()
                                    if (lat != null && lon != null) {
                                        scope.launch {
                                            preferencesManager.setManualLocation(lat, lon)
                                        }
                                    }
                                },
                                label = { Text(stringResource(R.string.latitude)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            
                            OutlinedTextField(
                                value = manualLon,
                                onValueChange = { 
                                    manualLon = it
                                    val lat = manualLat.toDoubleOrNull()
                                    val lon = it.toDoubleOrNull()
                                    if (lat != null && lon != null) {
                                        scope.launch {
                                            preferencesManager.setManualLocation(lat, lon)
                                        }
                                    }
                                },
                                label = { Text(stringResource(R.string.longitude)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Coordinate Precision Section
             Text(
                text = "Coordinate Precision",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    var precision by remember { mutableStateOf(4f) }
                    
                    LaunchedEffect(Unit) {
                        precision = preferencesManager.coordinatePrecision.first().toFloat()
                    }
                    
                    Text(
                        text = "Decimal digits: ${precision.toInt()}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Slider(
                        value = precision,
                        onValueChange = { precision = it },
                        onValueChangeFinished = {
                            scope.launch {
                                preferencesManager.setCoordinatePrecision(precision.toInt())
                            }
                        },
                        valueRange = 0f..4f,
                        steps = 3
                    )
                    
                    Text(
                        text = "Example: ${String.format("%.${precision.toInt()}f", 12.345678)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // About Section
            Text(
                text = stringResource(R.string.about),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val packageManager = context.packageManager
                    val packageName = context.packageName
                    val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
                    } else {
                        packageManager.getPackageInfo(packageName, 0)
                    }
                    val versionName = packageInfo.versionName
                    
                    Text(
                        text = stringResource(R.string.version, versionName ?: ""),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.app_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val disclaimer = stringResource(R.string.translation_disclaimer)
                    if (disclaimer.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = disclaimer,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.copyright),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
    
    // Language Selection Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.select_language)) },
            text = {

                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    supportedLanguages.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { lang ->
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedLanguage = lang.code
                                            scope.launch {
                                                preferencesManager.setLanguage(lang.code)
                                            }
                                            onLanguageChange(lang.code)
                                            showLanguageDialog = false
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedLanguage == lang.code,
                                        onClick = null
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Column {
                                        Text(
                                            text = lang.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = lang.nativeName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            },

            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
