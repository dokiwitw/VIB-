package com.mediquest.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mediquest.app.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VIBViewModel(application: Application) : AndroidViewModel(application) {

    private val _user = MutableStateFlow(User(id = "temp_${System.currentTimeMillis()}"))
    val user: StateFlow<User> = _user.asStateFlow()

    // Ranking Global vindo do Firebase
    val globalRanking: StateFlow<List<RankingEntry>> = UserRepository.getGlobalRanking()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _filtroAtivo = MutableStateFlow<CategoriaLocal?>(null)
    val filtroAtivo: StateFlow<CategoriaLocal?> = _filtroAtivo.asStateFlow()

    val hotspots: StateFlow<List<Hotspot>> = HotspotRepository.startRealtimeSync()
        .combine(_filtroAtivo) { list, filtro ->
            if (filtro == null) list else list.filter { it.categoria == filtro }
        }
        .combine(UserRepository.getActiveVibers()) { listaHotspots, vibersAtivos ->
            listaHotspots.map { hotspot ->
                // Conta quantos vibers ativos (não fantasmas) estão dentro de 100m do hotspot
                val vibersNoLocal = vibersAtivos.count { viber ->
                    calcularDistancia(viber.latitude, viber.longitude, hotspot.latitude, hotspot.longitude) < 100
                }

                if (vibersNoLocal > 0) {
                    // Lógica Automática (Crowd-Count): 1-2 pessoas = IDEAL, 3+ = LOTADO
                    val novaLotacao = when {
                        vibersNoLocal >= 3 -> LotacaoStatus.LOTADO
                        else -> LotacaoStatus.IDEAL
                    }
                    hotspot.copy(lotacaoAtual = novaLotacao)
                } else {
                    // Backup: Mantém a lotação vinda dos reportes/Google
                    hotspot
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Raio da Terra em metros
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    // Live Vibe: Vibers ativos no mapa
    val activeVibers: StateFlow<List<ActiveUser>> = UserRepository.getActiveVibers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _snackbarMensagem = MutableStateFlow<String?>(null)
    val snackbarMensagem: StateFlow<String?> = _snackbarMensagem.asStateFlow()

    private val geofenceManager = GeofenceManager(application)

    init {
        carregarOuCriarPerfil()
        
        // Escuta reportes vindos de notificações (Geofence)
        viewModelScope.launch {
            ReporteEventBus.eventos.collectLatest { reporte ->
                processarReporte(reporte)
            }
        }

        // Registra Geofences quando os hotspots são carregados (com debounce para evitar travamentos)
        viewModelScope.launch {
            var lastSize = 0
            hotspots.collectLatest { lista ->
                if (lista.size > lastSize) { // Só re-registra se crescer
                    lastSize = lista.size
                    delay(2000) // Debounce de 2s
                    geofenceManager.registrarGeofences(lista)
                }
            }
        }
    }

    private fun carregarOuCriarPerfil() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val localId = UserRepository.getLocalUserId(context)
            
            if (localId != null) {
                val perfilSalvo = UserRepository.getUser(localId)
                if (perfilSalvo != null) {
                    _user.value = perfilSalvo
                    return@launch
                }
            }
            
            // Se chegou aqui, é um novo usuário ou o ID expirou no banco
            val novoId = "user_${System.currentTimeMillis()}"
            val novoUsuario = User(
                id = novoId,
                nome = gerarNomeAleatorio()
            )
            _user.value = novoUsuario
            UserRepository.saveLocalUserId(context, novoId)
            UserRepository.saveUser(novoUsuario)
        }
    }

    private fun gerarNomeAleatorio(): String {
        val prefixos = listOf("Viber", "Night", "Party", "Urban", "Neon", "Street", "Midnight", "Cool")
        val sufixos = listOf("Walker", "Hunter", "Vibe", "King", "Queen", "Ghost", "Star", "Seeker")
        val num = (100..999).random()
        return "${prefixos.random()}${sufixos.random()}_$num"
    }

    fun updateUserName(novoNome: String) {
        viewModelScope.launch {
            val updatedUser = _user.value.copy(nome = novoNome)
            _user.value = updatedUser
            UserRepository.saveUser(updatedUser)
            _snackbarMensagem.value = "Nome atualizado com sucesso!"
        }
    }

    /**
     * Atualiza a posição do usuário no Live Vibe
     */
    fun updateLocation(lat: Double, lng: Double) {
        viewModelScope.launch {
            UserRepository.updateLivePosition(
                user = _user.value,
                lat = lat,
                lng = lng,
                avatarId = (_user.value.nivel % 5) + 1,
                photoBase64 = _user.value.photoBase64
            )
        }
    }

    fun updateProfilePhoto(base64: String) {
        viewModelScope.launch {
            val updatedUser = _user.value.copy(photoBase64 = base64)
            _user.value = updatedUser
            UserRepository.saveUser(updatedUser)
        }
    }

    fun toggleGhostMode(enabled: Boolean) {
        viewModelScope.launch {
            val updatedUser = _user.value.copy(isGhost = enabled)
            _user.value = updatedUser
            UserRepository.saveUser(updatedUser)
            
            // Força uma atualização imediata da posição (que vai deletar ou criar o marcador)
            val context = getApplication<Application>()
            // Aqui poderíamos forçar um refresh de localização, mas o loop da MainActivity cuidará disso em 30s
            
            _snackbarMensagem.value = if (enabled) "Modo Ghost Ativado! Você sumiu do mapa." 
                                     else "Modo Ghost Desativado! Você está visível."
        }
    }

    fun toggleVibPass(enabled: Boolean) {
        viewModelScope.launch {
            val updatedUser = _user.value.copy(hasVibPass = enabled)
            _user.value = updatedUser
            UserRepository.saveUser(updatedUser)
            _snackbarMensagem.value = if (enabled) "VIB! Pass Ativado! Aproveite seus benefícios." 
                                     else "VIB! Pass Desativado."
        }
    }

    fun setFiltro(categoria: CategoriaLocal?) {
        _filtroAtivo.value = categoria
    }

    fun buscarTudo() {
        viewModelScope.launch {
            _loading.value = true
            try {
                HotspotRepository.fetchFromGoogle(getApplication())
            } catch (e: Exception) {
                _snackbarMensagem.value = "Erro ao atualizar dados: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun processarReporte(reporte: ReporteLotacao) {
        viewModelScope.launch {
            val h = hotspots.value.find { it.id == reporte.hotspotId } ?: return@launch
            
            val pesoDoUsuario = 1.0 + (_user.value.nivel * 0.1)
            val novoPesoAgregado = h.pesoAgregado + (reporte.status.ordinal * pesoDoUsuario)
            val novoTotal = h.totalReportes + 1
            val media = (novoPesoAgregado / novoTotal).toInt()
            
            val novoStatus = LotacaoStatus.entries.getOrElse(media.coerceIn(0, 2)) { LotacaoStatus.VAZIO }

            val hotspotAtualizado = h.copy(
                lotacaoAtual = novoStatus,
                pesoAgregado = novoPesoAgregado,
                totalReportes = novoTotal
            )

            HotspotRepository.salvarReporteNoFirestore(hotspotAtualizado)

            // Atualiza XP e Sincroniza Usuário no Firebase
            val novoXP = _user.value.xp + 10
            val novoNivel = 1 + (novoXP.toInt() / 100)
            val usuarioAtualizado = _user.value.copy(xp = novoXP, nivel = novoNivel)
            _user.value = usuarioAtualizado
            
            UserRepository.saveUser(usuarioAtualizado)
            
            _snackbarMensagem.value = "Obrigado! Você ganhou 10 XP. Nível atual: $novoNivel"
        }
    }

    fun limparSnackbar() {
        _snackbarMensagem.value = null
    }
}
