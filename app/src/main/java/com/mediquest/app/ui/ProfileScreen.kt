package com.mediquest.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediquest.app.model.User
import com.mediquest.app.viewmodel.MediQuestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(vm: MediQuestViewModel, onBack: () -> Unit) {
    val user by vm.user.collectAsState()
    val progressoNivel = (user.xp % 100) / 100f
    val xpParaProximo  = 100 - (user.xp % 100)
    val pesoAtual      = 1.0 + user.nivel * 0.1

    var editingName by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(user.nome) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meu Perfil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("🎯", fontSize = 40.sp)
            }
            Spacer(Modifier.height(12.dp))

            if (editingName) {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Seu Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    trailingIcon = {
                        IconButton(onClick = { 
                            vm.updateUserName(tempName)
                            editingName = false 
                        }) {
                            Icon(Icons.Default.Check, "Salvar")
                        }
                    }
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(user.nome, style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold)
                    IconButton(onClick = { 
                        tempName = user.nome
                        editingName = true 
                    }) {
                        Icon(Icons.Default.Edit, "Editar Nome", modifier = Modifier.size(18.dp))
                    }
                }
            }

            Text("ID: ${user.id}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(24.dp))

            // XP Card
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⭐ Nível ${user.nivel}",
                            fontWeight = FontWeight.Bold, fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.primary)
                        Text("${user.xp} XP total",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress   = { progressoNivel.toFloat() },
                        modifier   = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color      = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Faltam $xpParaProximo XP para o nível ${user.nivel + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.75f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Confiança Card
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("⚖️ Peso de Confiança",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Seu reporte vale:")
                        Text(
                            "${"%.1f".format(pesoAtual)}×",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 20.sp
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Fórmula: 1.0 + (nível × 0.1) = 1.0 + (${user.nivel} × 0.1) = ${"%.1f".format(pesoAtual)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Continue reportando para ganhar XP e aumentar sua influência no mapa!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Tabela de progressão
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("📈 Tabela de Progressão",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    listOf(1, 5, 10, 20, 50).forEach { nivel ->
                        val peso = 1.0 + nivel * 0.1
                        val isAtual = nivel <= user.nivel && (nivel + 4) > user.nivel
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isAtual) Text("➤ ", color = MaterialTheme.colorScheme.primary)
                                Text(
                                    "Nível $nivel",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isAtual) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isAtual) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                "${"%.1f".format(peso)}× por reporte",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
