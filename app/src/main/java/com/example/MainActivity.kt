package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.example.data.PetRepository
import com.example.ui.MainScreen
import com.example.ui.PetViewModel
import com.example.ui.PetViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {

    private val appointmentIdState = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        handleIntent(intent)

        // Ensure Firebase is initialized
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        enableEdgeToEdge()

        // Initialize local PetRepository with context
        val repository = PetRepository(applicationContext)

        // Instantiate ViewModel with Custom Factory
        val viewModel: PetViewModel by viewModels {
            PetViewModelFactory(repository)
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel = viewModel, initialAppointmentId = appointmentIdState.value)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val id = intent?.getLongExtra("appointmentId", -1L) ?: -1L
        if (id != -1L) {
            appointmentIdState.value = id
        }
    }
}
