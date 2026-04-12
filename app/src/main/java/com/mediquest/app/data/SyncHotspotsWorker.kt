package com.mediquest.app.data

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

class SyncHotspotsWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME = "mq_sync_periodic"

        fun agendar(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncHotspotsWorker>(12, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
            Log.d(TAG, "Sincronização periódica agendada (12h).")
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Sincronizando hotspots...")
            // Tenta sincronizar com o Google. O repositório já cuida do cache de 24h.
            HotspotRepository.fetchFromGoogle(applicationContext)
            Log.d(TAG, "Sincronização concluída.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Falha na sincronização: ${e.message}")
            Result.retry()
        }
    }
}
