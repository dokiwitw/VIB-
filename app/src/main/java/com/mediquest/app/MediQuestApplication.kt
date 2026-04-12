package com.mediquest.app

import android.app.Application
import com.google.android.libraries.places.api.Places
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.mediquest.app.work.SyncHotspotsWorker

class MediQuestApplication : Application(), OnMapsSdkInitializedCallback {
    override fun onCreate() {
        super.onCreate()
        
        // Inicializa o Firebase (opcional se o google-services.json estiver correto, mas bom garantir)
        FirebaseApp.initializeApp(this)

        // Configuração Global do Firestore ANTES de qualquer uso
        val db = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()
        db.firestoreSettings = settings

        // Inicializa o Places API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        }

        // Força a inicialização do renderer mais moderno do Maps
        MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST, this)
        SyncHotspotsWorker.agendar(this)
    }

    override fun onMapsSdkInitialized(renderer: MapsInitializer.Renderer) {
        when (renderer) {
            MapsInitializer.Renderer.LATEST -> android.util.Log.d("MapsApp", "O renderizador mais recente está em uso.")
            MapsInitializer.Renderer.LEGACY -> android.util.Log.d("MapsApp", "O renderizador antigo está em uso.")
        }
    }
}
