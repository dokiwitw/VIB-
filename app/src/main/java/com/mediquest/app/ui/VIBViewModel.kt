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

    private val _user = MutableStateFlow(User(id = "user_${System.currentTimeMillis()}"))
    val user: StateFlow<User> = _user.asStateFlow()

    // Ranking Global vindo do Firebase
    val globalRanking: StateFlow<List<RankingEntry>> = UserRepository.getGlobalRanking()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _filtroAtivo = MutableStateFlow<CategoriaLocal?>(null)
    val filtroAtivo: StateFlow<CategoriaLocal?> = _filtroAtivo.asStateFlow()

    val hotspots: StateFlow<List<Hotspot>> = HotspotRepository.startRealtimeSync()
        .combine(_filtroAtivo) { list, filtro ->
            if (filtro == null) list else list.filter { it.categoria == filtro }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _snackbarMensagem = MutableStateFlow<String?>(null)
    val snackbarMensagem: StateFlow<String?> = _snackbarMensagem.asStateFlow()

    private val geofenceManager = GeofenceManager(application)

    init {
        // Salva o usuário inicial no Firebase para aparecer no ranking
        viewModelScope.launch {
            UserRepository.saveUser(_user.value)
        }

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

    fun updateUserName(novoNome: String) {
        viewModelScope.launch {
            val updatedUser = _user.value.copy(nome = novoNome)
            _user.value = updatedUser
            UserRepository.saveUser(updatedUser)
            _snackbarMensagem.value = "Nome atualizado com sucesso!"
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
