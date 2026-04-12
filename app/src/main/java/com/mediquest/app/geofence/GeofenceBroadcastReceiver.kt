package com.mediquest.app.geofence

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.mediquest.app.R
import com.mediquest.app.model.HotspotRepository
import com.mediquest.app.model.LotacaoStatus
import com.mediquest.app.model.ReporteLotacao
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

// ── Event Bus ─────────────────────────────────────────────────
object ReporteEventBus {
    private val _eventos = MutableSharedFlow<ReporteLotacao>(
        replay = 0, extraBufferCapacity = 10
    )
    val eventos: SharedFlow<ReporteLotacao> = _eventos
    fun publicar(r: ReporteLotacao) { _eventos.tryEmit(r) }
}

// ── Actions & Extras ──────────────────────────────────────────
object MQActions {
    const val ACTION_GEOFENCE  = "com.mediquest.app.ACTION_GEOFENCE_EVENT"
    const val ACTION_REPORTE   = "com.mediquest.app.ACTION_REPORTE_LOTACAO"
    const val EXTRA_HOTSPOT_ID = "extra_hotspot_id"
    const val EXTRA_NOME_LOCAL = "extra_nome_local"
    const val EXTRA_STATUS     = "extra_status"
    const val EXTRA_USER_ID    = "extra_user_id"
    const val CHANNEL_ID       = "mq_crowdsourcing"
    const val NOTIF_BASE       = 2000
}

// ── BroadcastReceiver ─────────────────────────────────────────
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            MQActions.ACTION_GEOFENCE -> onGeofenceEvent(context, intent)
            MQActions.ACTION_REPORTE  -> onReporte(context, intent)
        }
    }

    private fun onGeofenceEvent(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.e("GeoReceiver", "Erro: ${GeofenceStatusCodes.getStatusCodeString(event.errorCode)}")
            return
        }
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_DWELL) return

        event.triggeringGeofences?.forEach { gf ->
            val id   = gf.requestId
            val nome = HotspotRepository. vgetHotspot(id)?.nome ?: "Local Desconhecido"
            NotifHelper.dispararCrowdsourcing(context, id, nome)
        }
    }

    private fun onReporte(context: Context, intent: Intent) {
        val hotspotId = intent.getStringExtra(MQActions.EXTRA_HOTSPOT_ID) ?: return
        val statusStr = intent.getStringExtra(MQActions.EXTRA_STATUS) ?: return
        val userId    = intent.getStringExtra(MQActions.EXTRA_USER_ID) ?: "local-user-001"
        val status    = LotacaoStatus.entries.find { it.name == statusStr } ?: return

        ReporteEventBus.publicar(ReporteLotacao(hotspotId, status, userId))
        NotificationManagerCompat.from(context)
            .cancel(MQActions.NOTIF_BASE + hotspotId.hashCode())
    }
}

// ── Notification Helper ───────────────────────────────────────
object NotifHelper {

    fun dispararCrowdsourcing(
        context: Context,
        hotspotId: String,
        nomeLocal: String,
        userId: String = "local-user-001"
    ) {
        criarCanal(context)
        val notifId = MQActions.NOTIF_BASE + hotspotId.hashCode()

        fun acao(status: LotacaoStatus, reqCode: Int): PendingIntent {
            val i = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
                action = MQActions.ACTION_REPORTE
                putExtra(MQActions.EXTRA_HOTSPOT_ID, hotspotId)
                putExtra(MQActions.EXTRA_STATUS, status.name)
                putExtra(MQActions.EXTRA_USER_ID, userId)
            }
            // Importante: FLAG_MUTABLE para que o Intent possa ser entregue ao BroadcastReceiver com extras no Android 12+
            // No entanto, para Broadcasts com extras fixos, IMMUTABLE costuma bastar. 
            // Mas se os extras precisarem ser alterados pelo sistema (o que não é o caso aqui), seria MUTABLE.
            // Para garantir que os extras cheguem corretamente e seguindo a recomendação de segurança:
            return PendingIntent.getBroadcast(
                context, reqCode, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notif = NotificationCompat.Builder(context, MQActions.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Como está o movimento? 🎉")
            .setContentText("Como está o movimento no $nomeLocal?")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Você ficou em \"$nomeLocal\". Nos ajude a manter o mapa atualizado!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_vazio,  "🔵 Vazio",  acao(LotacaoStatus.VAZIO,  notifId + 1))
            .addAction(R.drawable.ic_ideal,  "🟡 Ideal",  acao(LotacaoStatus.IDEAL,  notifId + 2))
            .addAction(R.drawable.ic_lotado, "🔴 Lotado", acao(LotacaoStatus.LOTADO, notifId + 3))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notif)
        } catch (e: SecurityException) {
            Log.e("NotifHelper", "POST_NOTIFICATIONS negada: ${e.message}")
        }
    }

    private fun criarCanal(context: Context) {
        val ch = NotificationChannel(
            MQActions.CHANNEL_ID,
            "HangSpot — Reporte de Lotação",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Perguntas sobre o movimento em bares e baladas." }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(ch)
    }
}
