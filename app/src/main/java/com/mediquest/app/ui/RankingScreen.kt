package com.mediquest.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediquest.app.model.RankingEntry
import com.mediquest.app.model.User
import com.mediquest.app.viewmodel.MediQuestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
    vm: MediQuestViewModel,
    onBack: () -> Unit
) {
    val ranking by vm.globalRanking.collectAsState()
    val usuarioAtual by vm.user.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🏆 Ranking de Brasília") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⭐", fontSize = 28.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Seu Nível: ${usuarioAtual.nivel}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Peso do reporte: ${"%.1f".format(1.0 + usuarioAtual.nivel * 0.1)}x",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.75f)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${usuarioAtual.xp} XP",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            itemsIndexed(ranking) { index, entry ->
                val isVoce = entry.user.id == usuarioAtual.id
                val medalha = when (index) {
                    0 -> "🥇"
                    1 -> "🥈"
                    2 -> "🥉"
                    else -> "${index + 1}."
                }
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isVoce)
                            MaterialTheme.colorScheme.primaryContainer.copy(0.5f)
                        else
                            MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(if (isVoce) 4.dp else 1.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(medalha, fontSize = 20.sp, modifier = Modifier.width(36.dp))
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                entry.nome.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (isVoce) "Você" else entry.nome,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Nível ${entry.user.nivel} · ${entry.reportes} reportes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "${entry.user.xp} XP",
                            fontWeight = FontWeight.Bold,
                            color = if (index == 0) Color(0xFFFFD700)
                                    else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
