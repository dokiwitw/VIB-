package com.mediquest.app.data

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(private val context: Context) {

    companion object {
        const val TAG = "GeofenceManager"
        const val RAIO_METROS = 50f
        const val LOITERING_DELAY_MS = 600_000  // 10 minutos
    }

    private val client: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = MQActions.ACTION_GEOFENCE
        }
        PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    fun registrarGeofences(hotspots: List<Hotspot>) {
        val fineOk = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val bgOk = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineOk || !bgOk) {
            Log.w(TAG, "Permissões insuficientes — geofences não registradas.")
            return
        }

        // O Android limita cada app a 100 Geofences ativas.
        // Pegamos apenas os 90 mais relevantes para deixar margem de segurança.
        val geofencesLimitadas = hotspots
            .take(90) // Idealmente, aqui você filtraria pelos mais próximos do usuário
            .map { h ->
                Geofence.Builder()
                    .setRequestId(h.id)
                    .setCircularRegion(h.latitude, h.longitude, RAIO_METROS)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL)
                    .setLoiteringDelay(LOITERING_DELAY_MS)
                    .build()
            }

        if (geofencesLimitadas.isEmpty()) return

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL)
            .addGeofences(geofencesLimitadas)
            .build()

        client.addGeofences(request, pendingIntent)
            .addOnSuccessListener { Log.d(TAG, "${hotspots.size} geofences registradas.") }
            .addOnFailureListener { Log.e(TAG, "Erro ao registrar: ${it.message}") }
    }

    fun removerTodas() {
        client.removeGeofences(pendingIntent)
            .addOnSuccessListener { Log.d(TAG, "Geofences removidas.") }
    }
}
