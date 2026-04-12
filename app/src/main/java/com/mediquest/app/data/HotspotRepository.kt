package com.mediquest.app.data

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.SearchByTextRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Repositório Central de Hotspots (Pontos de Interesse).
 * Agora integrado com Firebase Firestore para sincronização em tempo real entre todos os usuários.
 */
object HotspotRepository {
    private val db = FirebaseFirestore.getInstance()
    private val hotspotsCollection = db.collection("hotspots")
    private val metadataCollection = db.collection("metadata")

    private val _hotspots = MutableStateFlow<Map<String, Hotspot>>(emptyMap())
    val hotspots: StateFlow<Map<String, Hotspot>> = _hotspots.asStateFlow()

    /**
     * Busca um hotspot localmente pelo ID.
     */
    fun getHotspot(id: String): Hotspot? = _hotspots.value[id]

    private val asaSulCenter = LatLng(-15.8147, -47.8919)
    private val asaNorteCenter = LatLng(-15.7631, -47.8711)

    /**
     * Escuta mudanças no Firestore em tempo real.
     */
    fun startRealtimeSync(): Flow<List<Hotspot>> = callbackFlow {
        val subscription = hotspotsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                try {
                    val hAbertura = doc.getLong("horaAbertura")?.toInt() ?: 18
                    val hFechamento = doc.getLong("horaFechamento")?.toInt() ?: 2
                    val categoria = CategoriaLocal.valueOf(doc.getString("categoria") ?: "OUTROS")
                    val numAvaliacoes = doc.getLong("numAvaliacoes")?.toInt() ?: 0
                    val nota = doc.getDouble("nota") ?: 0.0
                    val vibPassBenefits = doc.getString("vibPassBenefits")

                    // Recalcula a lotação em tempo real baseada no horário atual do dispositivo
                    val lotacaoCalculada = recalcularLotacaoDinamica(hAbertura, hFechamento, categoria, numAvaliacoes, nota)

                    Hotspot(
                        id = doc.id,
                        nome = doc.getString("nome") ?: "Local",
                        latitude = doc.getDouble("latitude") ?: 0.0,
                        longitude = doc.getDouble("longitude") ?: 0.0,
                        lotacaoAtual = lotacaoCalculada,
                        pesoAgregado = doc.getDouble("pesoAgregado") ?: 0.0,
                        totalReportes = doc.getLong("totalReportes")?.toInt() ?: 0,
                        categoria = categoria,
                        faixaPreco = FaixaPreco.valueOf(doc.getString("faixaPreco") ?: "MEDIO"),
                        horaAbertura = hAbertura,
                        horaFechamento = hFechamento,
                        endereco = doc.getString("endereco") ?: "Brasília, DF",
                        nota = nota,
                        numAvaliacoes = numAvaliacoes,
                        vibPassBenefits = vibPassBenefits
                    )
                } catch (e: Exception) { null }
            } ?: emptyList<Hotspot>()
            
            _hotspots.value = list.associateBy { it.id }
            trySend(list)
        }
        awaitClose { subscription.remove() }
    }

    /**
     * Envia um reporte para a nuvem.
     */
    suspend fun salvarReporteNoFirestore(hotspot: Hotspot) {
        hotspotsCollection.document(hotspot.id).set(
            mapOf(
                "nome" to hotspot.nome,
                "latitude" to hotspot.latitude,
                "longitude" to hotspot.longitude,
                "lotacaoAtual" to hotspot.lotacaoAtual.name,
                "pesoAgregado" to hotspot.pesoAgregado,
                "totalReportes" to hotspot.totalReportes,
                "categoria" to hotspot.categoria.name,
                "faixaPreco" to hotspot.faixaPreco.name,
                "horaAbertura" to hotspot.horaAbertura,
                "horaFechamento" to hotspot.horaFechamento,
                "endereco" to hotspot.endereco,
                "nota" to hotspot.nota,
                "numAvaliacoes" to hotspot.numAvaliacoes,
                "vibPassBenefits" to hotspot.vibPassBenefits,
                "ultimaAtualizacao" to System.currentTimeMillis()
            ),
            SetOptions.merge()
        ).await()
    }

    /**
     * Busca inicial do Google Places integrado com horários reais.
     * Agora com CACHE DE 24 HORAS e ESCRITA EM LOTE para performance.
     */
    suspend fun fetchFromGoogle(context: Context, forceRefresh: Boolean = false): List<Hotspot> = withContext(Dispatchers.IO) {
        // 1. Verifica se precisamos atualizar (Cache de 24h)
        if (!forceRefresh) {
            try {
                val meta = metadataCollection.document("last_sync").get().await()
                val lastSync = meta.getLong("timestamp") ?: 0L
                val vinteQuatroHoras = 24 * 60 * 60 * 1000L
                
                if (System.currentTimeMillis() - lastSync < vinteQuatroHoras) {
                    return@withContext _hotspots.value.values.toList()
                }
            } catch (e: Exception) { }
        }

        val placesClient = Places.createClient(context)
        val fields = listOf(
            Place.Field.ID, 
            Place.Field.DISPLAY_NAME, 
            Place.Field.LOCATION, 
            Place.Field.TYPES,
            Place.Field.OPENING_HOURS,
            Place.Field.CURRENT_OPENING_HOURS,
            Place.Field.BUSINESS_STATUS,
            Place.Field.PRICE_LEVEL,
            Place.Field.ADDRESS,
            Place.Field.RATING,
            Place.Field.USER_RATINGS_TOTAL
        )
        
        val buscaConfig = mapOf(
            CategoriaLocal.BAR to listOf("bares"),
            CategoriaLocal.RESTAURANTE to listOf("restaurantes"),
            CategoriaLocal.BALADA to listOf("baladas"),
            CategoriaLocal.CAFE to listOf("cafeterias"),
            CategoriaLocal.PARQUE to listOf("parques"),
            CategoriaLocal.SHOPPING to listOf("shoppings")
        )

        val subSetores = listOf("Asa Sul", "Asa Norte")

        val results = coroutineScope {
            subSetores.flatMap { bairro ->
                val center = if (bairro == "Asa Sul") asaSulCenter else asaNorteCenter
                val bias = CircularBounds.newInstance(center, 5000.0)

                buscaConfig.flatMap { (catEnum, termos) ->
                    termos.map { termo ->
                        async {
                            val searchQuery = "$termo em $bairro, Brasília"
                            val request = SearchByTextRequest.builder(searchQuery, fields)
                                .setMaxResultCount(10)
                                .setLocationBias(bias)
                                .build()

                            try {
                                val response = placesClient.searchByText(request).await()
                                response.places.map { place ->
                                    val (hAbertura, hFechamento) = extrairHorarios(place)
                                    val lotacaoCalculada = estimarLotacaoComPlaces(place, catEnum)
                                    
                                    Hotspot(
                                        id = place.id ?: "unknown",
                                        nome = place.displayName ?: "Local",
                                        latitude = place.location?.latitude ?: 0.0,
                                        longitude = place.location?.longitude ?: 0.0,
                                        lotacaoAtual = lotacaoCalculada,
                                        pesoAgregado = calcularPesoDePopularidade(place),
                                        categoria = catEnum,
                                        faixaPreco = when (place.priceLevel ?: 2) {
                                            1 -> FaixaPreco.BARATO
                                            2 -> FaixaPreco.MEDIO
                                            3 -> FaixaPreco.CARO
                                            4 -> FaixaPreco.VIP
                                            else -> FaixaPreco.MEDIO
                                        },
                                        horaAbertura = hAbertura,
                                        horaFechamento = hFechamento,
                                        endereco = place.address ?: "Brasília, DF",
                                        nota = place.rating ?: 0.0,
                                        numAvaliacoes = place.userRatingsTotal ?: 0
                                    )
                                }
                            } catch (e: Exception) { emptyList<Hotspot>() }
                        }
                    }
                }
            }.awaitAll().flatten().distinctBy { it.id }
        }

        // 2. Escrita em Lote (Write Batch) - CRITICAL: Evita travar a UI com 100+ atualizações individuais
        if (results.isNotEmpty()) {
            val batch = db.batch()
            results.forEach { h ->
                val docRef = hotspotsCollection.document(h.id)
                batch.set(docRef, mapOf(
                    "nome" to h.nome,
                    "latitude" to h.latitude,
                    "longitude" to h.longitude,
                    "lotacaoAtual" to h.lotacaoAtual.name,
                    "pesoAgregado" to h.pesoAgregado,
                    "totalReportes" to h.totalReportes,
                    "categoria" to h.categoria.name,
                    "faixaPreco" to h.faixaPreco.name,
                    "horaAbertura" to h.horaAbertura,
                    "horaFechamento" to h.horaFechamento,
                    "endereco" to h.endereco,
                    "nota" to h.nota,
                    "numAvaliacoes" to h.numAvaliacoes,
                    "vibPassBenefits" to h.vibPassBenefits,
                    "ultimaAtualizacao" to System.currentTimeMillis()
                ), SetOptions.merge())
            }
            batch.set(metadataCollection.document("last_sync"), mapOf("timestamp" to System.currentTimeMillis()))
            batch.commit().await()
        }
        
        results
    }

    /**
     * Lógica centralizada para decidir a lotação em tempo real.
     * Se o local está fora do horário de funcionamento, a lotação é OBRIGATORIAMENTE VAZIO.
     */
    private fun recalcularLotacaoDinamica(
        hAbertura: Int,
        hFechamento: Int,
        categoria: CategoriaLocal,
        numAvaliacoes: Int,
        nota: Double
    ): LotacaoStatus {
        val agora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        
        // Verifica se está aberto
        val estaAberto = if (hFechamento > hAbertura) {
            agora in hAbertura until hFechamento
        } else {
            agora >= hAbertura || agora < hFechamento
        }

        if (!estaAberto) return LotacaoStatus.VAZIO

        // Se está aberto, aplica a lógica de pico
        val noPico = when (categoria) {
            CategoriaLocal.BAR, CategoriaLocal.BALADA -> agora >= 21 || agora <= 2
            CategoriaLocal.RESTAURANTE -> agora in 12..14 || agora in 20..22
            CategoriaLocal.SHOPPING -> agora in 16..21
            CategoriaLocal.CAFE -> agora in 8..11 || agora in 15..17
            else -> agora in 10..18
        }

        return when {
            !noPico -> LotacaoStatus.VAZIO
            numAvaliacoes > 1000 && nota > 4.2 -> LotacaoStatus.LOTADO
            numAvaliacoes > 200 -> LotacaoStatus.IDEAL
            else -> LotacaoStatus.VAZIO
        }
    }

    /**
     * Estima a lotação baseada em dados reais do Places ao buscar novos locais.
     */
    private fun estimarLotacaoComPlaces(place: Place, categoria: CategoriaLocal): LotacaoStatus {
        val (hAbertura, hFechamento) = extrairHorarios(place)
        return recalcularLotacaoDinamica(
            hAbertura, 
            hFechamento, 
            categoria, 
            place.userRatingsTotal ?: 0, 
            place.rating ?: 0.0
        )
    }

    /**
     * Calcula um peso para o Heatmap (0.0 a 1.0) baseado na "densidade" do local.
     * Lugares com 5.000 avaliações brilham mais que lugares com 10.
     */
    private fun calcularPesoDePopularidade(place: Place): Double {
        val ratings = place.userRatingsTotal ?: 0
        val rating = place.rating ?: 0.0
        
        // Normaliza a popularidade (ex: 2000+ avaliações = peso máximo)
        val fatorTamanho = (ratings.toDouble() / 2000.0).coerceAtMost(1.0)
        val fatorQualidade = (rating / 5.0)
        
        return (fatorTamanho * 0.7 + fatorQualidade * 0.3).coerceIn(0.1, 1.0)
    }

    private fun extrairHorarios(place: Place): Pair<Int, Int> {
        val openingHours = place.openingHours ?: return Pair(18, 2)
        val periods = openingHours.periods
        
        // Google uses Calendar.DAY_OF_WEEK (1=Sun, 2=Mon...) but openingHours.periods use DayOfWeek enum or similar
        // Let's simplify and get the period for the current day.
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK) // 1 (SUN) to 7 (SAT)
        
        // Map Calendar DAY_OF_WEEK to what Google uses (usually 0=SUN or similar)
        val targetDay = currentDay - 1 

        val period = periods.find { it.open?.day?.ordinal == targetDay } ?: periods.firstOrNull()
        
        val openHour = period?.open?.time?.hours ?: 18
        val closeHour = period?.close?.time?.hours ?: 2
        
        return Pair(openHour, closeHour)
    }
}
