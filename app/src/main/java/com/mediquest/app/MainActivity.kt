package com.mediquest.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.mediquest.app.ui.AppNavigation
import com.mediquest.app.ui.theme.VIBTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            
            // Gerenciador de Permissões Críticas (VIB! requer Localização para Heatmap e Geofences)
            val permissionsToRequest = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val allGranted = permissions.values.all { it }
                if (allGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Solicita permissão de segundo plano separadamente (exigência do Android)
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) 
                        != PackageManager.PERMISSION_GRANTED) {
                        // Opcional: Mostrar diálogo explicando por que precisamos de background
                    }
                }
            }

            LaunchedEffect(Unit) {
                launcher.launch(permissionsToRequest.toTypedArray())
            }

            VIBTheme {
                AppNavigation()
            }
        }
    }
}
