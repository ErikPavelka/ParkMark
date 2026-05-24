package com.example.parkmark.ui.screens

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.view.SoundEffectConstants
import android.view.View
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.parkmark.viewmodel.ParkingViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.math.exp
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun MapScreen(navController: NavController, viewModel: ParkingViewModel) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val history by viewModel.parkingHistory.collectAsState()

    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val sharedPreferences =
        remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var showSaveDialog by remember { mutableStateOf(false) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            // Ak sa používateľ práve vrátil do aplikácie (napr. z nastavení)
            if (event == Lifecycle.Event.ON_RESUME) {
                hasLocationPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val defaultLocation = LatLng(49.2231, 18.7394)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 14f)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {
                isGranted ->
            if(!isGranted) {
                Toast.makeText(context, "bez povolenia nie je možné zobraziť časovač", Toast.LENGTH_LONG).show()
            }
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasLocationPermission = isGranted
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val hasNotifPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

                if (!hasNotifPermission) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    )
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        cameraPositionState.position =
                            CameraPosition.fromLatLngZoom(userLatLng, 16f)
                    }
                }
            } catch (e: SecurityException) {

            }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }


    val mapProperties = MapProperties(isMyLocationEnabled = hasLocationPermission)
    val uiSettings = MapUiSettings(myLocationButtonEnabled = hasLocationPermission)
    val parking = history.maxByOrNull { it.startTime }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ParkMark") },
                actions = {
                    IconButton(onClick = {
                        navController.navigate("history")
                        if (sharedPreferences.getBoolean("vibration_enabled", true)) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (sharedPreferences.getBoolean("sound_enabled", true)) view.playSoundEffect(SoundEffectConstants.CLICK)
                    }) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "history"
                        )
                    }
                    IconButton(onClick = {
                        navController.navigate("settings")
                        if (sharedPreferences.getBoolean("vibration_enabled", true)) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (sharedPreferences.getBoolean("sound_enabled", true)) view.playSoundEffect(SoundEffectConstants.CLICK)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "settings"
                        )
                    }
                },
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = uiSettings
            ) {
                if (parking != null) {
                    val parkingLocation = LatLng(parking.latitude, parking.longitude)
                    Marker(
                        state = rememberMarkerState(position = parkingLocation),
                        title = "parked car",
                        snippet = parking.address
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                LargeFloatingActionButton(
                    onClick = {
                        if (sharedPreferences.getBoolean("vibration_enabled", true)) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (sharedPreferences.getBoolean("sound_enabled", true)) view.playSoundEffect(SoundEffectConstants.CLICK)

                        if (hasLocationPermission) {
                            showSaveDialog = true
                        } else {
                            Toast.makeText(
                                context,
                                "Poloha je zakázaná. Povoľte v nastaveniach.",
                                Toast.LENGTH_LONG
                            ).show()
                            val intent =
                                android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .apply {
                                        data = android.net.Uri.fromParts(
                                            "package",
                                            context.packageName,
                                            null
                                        )
                                    }
                            context.startActivity(intent)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "save location",
                        modifier = Modifier.size(75.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (parking != null) {
                        FloatingActionButton(
                            onClick = {
                                val uri =
                                    android.net.Uri.parse("google.navigation:q=${parking.latitude},${parking.longitude}&mode=w")
                                try {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        uri
                                    )
                                    intent.setPackage("com.google.android.apps.maps")
                                    context.startActivity(intent)
                                } catch (e: IOException) {
                                    Toast.makeText(
                                        context,
                                        "nie su nainstalovane google maps",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                if (sharedPreferences.getBoolean("vibration_enabled", true)) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (sharedPreferences.getBoolean("sound_enabled", true)) view.playSoundEffect(SoundEffectConstants.CLICK)

                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "navigovat k autu"
                            )
                        }
                    }
                }
            }
        }
    }
    if (showSaveDialog) {
        SaveParkingDialog(
            onDismissRequest = { showSaveDialog = false },
            onSaveRequest = { note, addedHours, addedMinutes ->
                showSaveDialog = false

                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                val geocoder = android.location.Geocoder(
                                    context,
                                    java.util.Locale.getDefault()
                                )
                                var address = "unknown address"

                                try {
                                    val addresses = geocoder.getFromLocation(
                                        location.latitude,
                                        location.longitude,
                                        1
                                    )
                                    if (!addresses.isNullOrEmpty()) {
                                        address =
                                            addresses[0].getAddressLine(0) ?: "address not found"
                                    }
                                } catch (e: IOException) {

                                }
                                val expireTime = if (addedHours > 0 || addedMinutes > 0) {
                                    System.currentTimeMillis() + addedHours.hours.inWholeMilliseconds + addedMinutes.minutes.inWholeMilliseconds
                                } else {
                                    null
                                }

                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    viewModel.saveParking(
                                        latitude = location.latitude,
                                        longitude = location.longitude,
                                        address = address,
                                        note = note.ifEmpty { "zaparkované" },
                                        expireTime = expireTime
                                    )
                                    if (expireTime != null) {
                                        showParkingNotification(
                                            context = context,
                                            note = note.ifEmpty { "bez poznámky" },
                                            expireTime = expireTime
                                        )
                                    }
                                    Toast.makeText(
                                        context,
                                        "Poloha uložená",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "GPS nefunguje.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: SecurityException) {
                    Toast.makeText(
                        context,
                        "Chýba povolenie na polohu",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }


            },
            haptic = haptic,
            view = view,
            sharedPreferences = sharedPreferences
        )
    }
}



@Composable
fun SaveParkingDialog(onDismissRequest : () -> Unit,
                      onSaveRequest :(note: String, addedHours: Int, addedMinutes : Int) -> Unit,
                      haptic: HapticFeedback,
                      view : View,
                      sharedPreferences: SharedPreferences) {
    var noteText by remember { mutableStateOf("") }
    var selectedHours by remember { mutableStateOf(0) }
    var selectedMinutes by remember { mutableStateOf(0) }

    val isDarkMode = isSystemInDarkTheme()
    AlertDialog (
        onDismissRequest = onDismissRequest,
        title = { Text("Uložiť parkovanie")},
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        text = {
            Column (
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = {noteText = it},
                    label = {Text("Poznámka napr. sektor A")},
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Dĺžka parkovania",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AndroidView(
                        factory = {context ->
                            NumberPicker(context).apply {
                                minValue = 0
                                maxValue = 24
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                    textColor = if (isDarkMode) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                                }
                                setOnValueChangedListener { _, _, newVal -> selectedHours = newVal
                                if (sharedPreferences.getBoolean("vibration_enabled", true)) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (sharedPreferences.getBoolean("sound_enabled", true)) view.playSoundEffect(SoundEffectConstants.CLICK)
                                }

                            }
                        }
                    )
                    Text("hod", modifier = Modifier.padding(horizontal = 8.dp))

                    AndroidView(
                        factory = {context ->
                            NumberPicker(context).apply {
                                minValue = 0
                                maxValue = 59
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                    textColor = if (isDarkMode) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                                }
                                setOnValueChangedListener { _, _, newVal -> selectedMinutes = newVal
                                    if (sharedPreferences.getBoolean("vibration_enabled", true)) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (sharedPreferences.getBoolean("sound_enabled", true)) view.playSoundEffect(SoundEffectConstants.CLICK)
                                }
                            }
                        }
                    )
                    Text("min")
                }
            }
        },
        confirmButton = {
            TextButton (
                onClick = {
                    if (sharedPreferences.getBoolean("vibration_enabled", true)) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (sharedPreferences.getBoolean("sound_enabled", true)) view.playSoundEffect(SoundEffectConstants.CLICK)
                    onSaveRequest(noteText, selectedHours, selectedMinutes)
                }
            ) {
                Text("Uložiť")
            }
        },
        dismissButton = {
            TextButton(

                onClick = {
                    if (sharedPreferences.getBoolean("vibration_enabled", true)) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (sharedPreferences.getBoolean("sound_enabled", true)) view.playSoundEffect(SoundEffectConstants.CLICK)
                    onDismissRequest()
                }
            ) {
                Text("Zrušiť")
            }
        }

    )
}
fun showParkingNotification(context : Context, note : String, expireTime : Long) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "parking_timer_channel"

    if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "časovač parkovania",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "zostavajúci čas parkovacieho lístka"
        }
        notificationManager.createNotificationChannel(channel)
    }
    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_lock_lock)
        .setContentTitle("aktívne parkovanie")
        .setContentText("poznámka: $note")
        .setOngoing(true)
        .setUsesChronometer(true)
        .setChronometerCountDown(true)
        .setWhen(expireTime)
        .setPriority(NotificationCompat.PRIORITY_LOW)

    notificationManager.notify(100, builder.build())

}
fun cancelParkingNotification(context : Context) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancel(100)
}