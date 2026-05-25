package com.example.parkmark.ui.screens


import android.content.Context
import android.view.SoundEffectConstants
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
//pomahal som si s AI pri rieseni nastavenia pre zvuk, vibracie a ako tie nastavenia ukladat
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, viewModel: ParkingViewModel) {
    val context = LocalContext.current

    val sharedPreferences = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    var isVibrationEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("vibration_enabled", true)) }
    var isSoundEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("sound_enabled", true)) }
    var isAutoDelete30Days by remember {mutableStateOf(sharedPreferences.getBoolean("auto_delete_30_days", false))}


    Scaffold(
        topBar = {
            TopAppBar(
                title = {Text(stringResource(R.string.nastavenia))},
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                            val isVibrationEnabled = sharedPreferences.getBoolean("vibration_enabled", true)
                            val isSoundEnabled = sharedPreferences.getBoolean("sound_enabled", true)
                            if (isVibrationEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            if (isSoundEnabled) {
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.spat)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) {
        paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.zvuky_a_vibracie),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.vibracie),
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = isVibrationEnabled,
                    onCheckedChange = { value ->
                        isVibrationEnabled = value
                        sharedPreferences.edit().putBoolean("vibration_enabled", value).apply()
                        val isVibrationEnabled = sharedPreferences.getBoolean("vibration_enabled", true)
                        val isSoundEnabled = sharedPreferences.getBoolean("sound_enabled", true)
                        if (isVibrationEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        if (isSoundEnabled) {
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                        }
                    }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.zvuky),
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = isSoundEnabled,
                    onCheckedChange = {value ->
                        isSoundEnabled = value
                        sharedPreferences.edit().putBoolean("sound_enabled", value).apply()
                    }
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = stringResource(R.string.historia),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.vymazat_historiu),
                    style = MaterialTheme.typography.bodyLarge
                )
                Button(
                    onClick = {
                        viewModel.clearHistory()
                        cancelParkingNotification(context)
                        Toast.makeText(context,
                            context.getString(R.string.vymazala_sa_historia), Toast.LENGTH_LONG).show()
                        val isVibrationEnabled = sharedPreferences.getBoolean("vibration_enabled", true)
                        val isSoundEnabled = sharedPreferences.getBoolean("sound_enabled", true)
                        if (isVibrationEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        if (isSoundEnabled) {
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                        }
                    },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(
                        text = stringResource(R.string.vymaz),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.automaticke_mazanie_historie),
                    style = MaterialTheme.typography.bodyLarge
                )
                Button(
                    onClick = {
                        val newValue = !isAutoDelete30Days
                        isAutoDelete30Days = newValue
                        sharedPreferences.edit().putBoolean("auto_delete_30_days", newValue).apply()
                        if (newValue) {
                            viewModel.deleteOldHistory()
                            Toast.makeText(context,
                                context.getString(R.string.zapnuta_star_historia_bola_vymazana), Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAutoDelete30Days) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isAutoDelete30Days) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        text = if (isAutoDelete30Days) stringResource(R.string.t30_dni_ano) else stringResource(
                            R.string.t30_dni_nie
                        ),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}