package com.example.parkmark.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Button
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.traceEventStart
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
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

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun MapScreen(navController: NavController, viewModel: ParkingViewModel) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val history by viewModel.parkingHistory.collectAsState()

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasLocationPermission = isGranted}
    )

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(userLatLng, 16f)
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
                title = {Text("ParkMark")},
                actions = {
                    IconButton(onClick = {navController.navigate("history")}) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "history"
                        )
                    }
                    IconButton(onClick = {navController.navigate("settings")}) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "settings"
                        )
                    }
                },
                colors = topAppBarColors (
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
                        state = rememberMarkerState(position =  parkingLocation),
                        title = "parked car",
                        snippet = parking.address
                    )
                }
            }
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                LargeFloatingActionButton(
                    onClick = {
                        if (hasLocationPermission) {
                            try {
                                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                    if (location != null) {
                                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                                            var address = "unknown address"

                                            try {
                                                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                                if (!addresses.isNullOrEmpty()) {
                                                    address = addresses[0].getAddressLine(0) ?: "address not found"
                                                }
                                            } catch (e : IOException) {

                                            }
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                viewModel.saveParking(
                                                    latitude = location.latitude,
                                                    longitude = location.longitude,
                                                    address = address,
                                                    note = "zaparkovane",
                                                    expireTime = null
                                                )
                                                Toast.makeText(
                                                    context,
                                                    "Parking location saved",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "GPS nefunguje. testovaci zaznam zilina",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } catch (e: SecurityException) {
                                Toast.makeText(context, "Chýba povolenie na polohu", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // Informujeme používateľa, prečo to nefunguje
                            Toast.makeText(
                                context,
                                "Poloha je zakázaná. Povoľte ju v nastaveniach aplikácie.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()

                            // Otvoríme systémové nastavenia priamo na detaile našej aplikácie
                            val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.fromParts("package", context.packageName, null)
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
                                val uri = android.net.Uri.parse("google.navigation:q=${parking.latitude},${parking.longitude}&mode=w")
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                    intent.setPackage("com.google.android.apps.maps")
                                    context.startActivity(intent)
                                } catch (e: IOException) {
                                    Toast.makeText(
                                        context,
                                        "nie su nainstalovane google maps",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
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
}