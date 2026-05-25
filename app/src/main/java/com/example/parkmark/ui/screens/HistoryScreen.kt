package com.example.parkmark.ui.screens


import android.content.Context
import android.view.SoundEffectConstants
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.parkmark.R
import com.example.parkmark.viewmodel.ParkingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController, viewModel : ParkingViewModel) {
    val history by viewModel.parkingHistory.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val sharedPreferences = remember {context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)}

    Scaffold(
        topBar = {
            TopAppBar(
                title = {Text(stringResource(R.string.historia_parkovania)) },
            navigationIcon = {
                IconButton(
                    onClick = {navController.popBackStack()
                        if (sharedPreferences.getBoolean("vibration_enabled", true)) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (sharedPreferences.getBoolean("sound_enabled", true)) view.playSoundEffect(SoundEffectConstants.CLICK)
                    }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.spat)
                    )
                }
            },
                colors = topAppBarColors (
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ))
        }
    ) {
        paddingValues ->
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.zatial_nemas_ulozene_ziadne_parkovania))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(history.sortedByDescending { it.startTime }) {record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                val date = Date(record.startTime)
                                val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                                val dateString= format.format(date)

                                Text(
                                    text = stringResource(R.string.datum, dateString),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = stringResource(R.string.adresa, record.address))
                                if (record.note.isNotEmpty() && record.note != stringResource(R.string.zaparkovane)) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.poznamka, record.note),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (record.expireTime != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val expireDate = Date(record.expireTime)
                                    val expireString = format.format(expireDate)
                                    val isExpired = record.expireTime < System.currentTimeMillis()

                                    Text(
                                        text = if (isExpired) stringResource(
                                            R.string.listok_vyprsal,
                                            expireString
                                        ) else stringResource(R.string.listok_plati, expireString),
                                        fontWeight = FontWeight.Bold,
                                        color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    viewModel.deleteParking(record)
                                    cancelParkingNotification(context)
                                    if (sharedPreferences.getBoolean("vibration_enabled", true)) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (sharedPreferences.getBoolean("sound_enabled", true)) view.playSoundEffect(SoundEffectConstants.CLICK)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.vymazat),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}