package com.example.parkmark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkmark.data.ParkingDatabase
import com.example.parkmark.ui.AppNavigation
import com.example.parkmark.ui.theme.ParkMarkTheme
import com.example.parkmark.viewmodel.ParkingViewModel
import com.example.parkmark.viewmodel.ParkingViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ParkMarkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background)
                {
                    val database = ParkingDatabase.getDatabase(applicationContext)
                    val dao = database.parkingDao()

                    val viewModel: ParkingViewModel = viewModel(factory = ParkingViewModelFactory(dao))


                    AppNavigation(viewModel = viewModel)
                }

            }
        }
    }
}