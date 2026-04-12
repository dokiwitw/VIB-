package com.mediquest.app.data

import java.util.Calendar

/**
 * Representa os estados possíveis de ocupação de um local.
 */
enum class LotacaoStatus {
    VAZIO,
    IDEAL,
    LOTADO
}

/**
 * Categorias principais para o sistema de filtros.
 */
enum class CategoriaLocal(val displayName: String) {
    BAR("Bares"),
    RESTAURANTE("Restaurantes"),
    BALADA("Baladas"),
    FAST_FOOD("Fast Food"),
    CAFE("Cafés"),
    SHOWS("Shows & Eventos"),
    PARQUE("Parques & Lazer"),
    SHOPPING("Shoppings"),
    OUTROS("Outros")
}

/**
 * Faixas de preço para o local.
 */
enum class FaixaPreco(val label: String) {
    BARATO("$"),
    MEDIO("$$"),
    CARO("$$$"),
    VIP("$$$$")
}

/**
 * Entidade principal que representa um Ponto de Interesse (Hotspot) no mapa.
 */
data class Hotspot(
    val id: String,
    val nome: String,
    val latitude: Double,
    val longitude: Double,
    val lotacaoAtual: LotacaoStatus = LotacaoStatus.VAZIO,
    val pesoAgregado: Double = 0.0,
    val totalReportes: Int = 0,
    val categoria: CategoriaLocal = CategoriaLocal.OUTROS,
    val faixaPreco: FaixaPreco = FaixaPreco.MEDIO,
    val horaAbertura: Int = 18, // 0-23
    val horaFechamento: Int = 2,  // 0-23
    val endereco: String = "Brasília, DF",
    val nota: Double = 0.0,
    val numAvaliacoes: Int = 0,
    val vibPassBenefits: String? = null // Benefícios do VIB! Pass (ex: "1 Drink Grátis")
) {
    fun estaAberto(): Boolean {
        val agora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (horaFechamento > horaAbertura) {
            agora in horaAbertura until horaFechamento
        } else {
            // Caso vire a noite (ex: abre 18h, fecha 04h)
            agora >= horaAbertura || agora < horaFechamento
        }
    }
}

/**
 * Representa um usuário ativo no mapa em tempo real (Estilo Waze).
 */
data class ActiveUser(
    val id: String = "",
    val nome: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val avatarId: Int = 1, // ID do avatar selecionado
    val photoBase64: String? = null, // Foto do usuário em Base64 para economizar Cloud Storage
    val isGhost: Boolean = false, // Se true, não aparece para os outros
    val lastUpdate: Long = System.currentTimeMillis()
)

/**
 * Representa o Perfil do Usuário e seu progresso de Gamificação.
 */
data class User(
    val id: String,
    val nome: String = "Explorador",
    val xp: Long = 0L,
    val nivel: Int = 1,
    val avatarId: Int = 1,
    val photoBase64: String? = null,
    val isGhost: Boolean = false,
    val hasVibPass: Boolean = false // Status de assinatura do VIB! Pass
)

/**
 * Entrada para o Ranking Global.
 */
data class RankingEntry(
    val user: User,
    val nome: String,
    val reportes: Int
)

/**
 * Registro de um reporte individual enviado por um usuário.
 */
data class ReporteLotacao(
    val hotspotId: String,
    val status: LotacaoStatus,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis()
)
