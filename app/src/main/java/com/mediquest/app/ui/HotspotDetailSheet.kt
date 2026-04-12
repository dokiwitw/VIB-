package com.mediquest.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediquest.app.model.Hotspot
import com.mediquest.app.model.LotacaoStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotspotDetailSheet(
    hotspot: Hotspot,
    onDismiss: () -> Unit,
    onReporte: (LotacaoStatus) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val estaAberto = hotspot.estaAberto()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            // Cabeçalho: Nome e Status de Abertura
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = hotspot.nome,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = hotspot.categoria.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Badge de Aberto/Fechado
                Surface(
                    color = if (estaAberto) Color(0xFF4CAF50).copy(0.1f) else Color.Red.copy(0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (estaAberto) "ABERTO AGORA" else "FECHADO",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (estaAberto) Color(0xFF4CAF50) else Color.Red
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Info Cards (Tipo, Preço, Horário)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoChip(Icons.Default.Payments, hotspot.faixaPreco.label, Modifier.weight(1f))
                InfoChip(Icons.Default.Schedule, "${hotspot.horaAbertura}h - ${hotspot.horaFechamento}h", Modifier.weight(1.5f))
                InfoChip(Icons.Default.Star, "${hotspot.nota} (${hotspot.numAvaliacoes})", Modifier.weight(1.2f))
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            // Endereço Real
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = hotspot.endereco,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(24.dp))

            // Seção de Lotação em Tempo Real
            Text(
                "MOVIMENTO EM TEMPO REAL",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            // Status Atual do Heatmap
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = getStatusColor(hotspot.lotacaoAtual).copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(12.dp).background(getStatusColor(hotspot.lotacaoAtual), CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = getStatusText(hotspot.lotacaoAtual),
                        fontWeight = FontWeight.Bold,
                        color = getStatusColor(hotspot.lotacaoAtual)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "${hotspot.totalReportes} reportes hoje",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Ações de Reporte (Crowdsourcing)
            Text(
                "AJUDE A COMUNIDADE. COMO ESTÁ O LUGAR?",
                style = MaterialTheme.typography.labelMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ReportButton("Tranquilo", LotacaoStatus.VAZIO, Color.Cyan, Modifier.weight(1f)) {
                    onReporte(LotacaoStatus.VAZIO)
                    onDismiss()
                }
                ReportButton("Normal", LotacaoStatus.IDEAL, Color.Green, Modifier.weight(1f)) {
                    onReporte(LotacaoStatus.IDEAL)
                    onDismiss()
                }
                ReportButton("Lotado", LotacaoStatus.LOTADO, Color.Red, Modifier.weight(1f)) {
                    onReporte(LotacaoStatus.LOTADO)
                    onDismiss()
                }
            }
        }
    }
}

@Composable
fun InfoChip(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ReportButton(label: String, status: LotacaoStatus, color: Color, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

fun getStatusColor(status: LotacaoStatus) = when(status) {
    LotacaoStatus.VAZIO -> Color.Cyan
    LotacaoStatus.IDEAL -> Color.Green
    LotacaoStatus.LOTADO -> Color.Red
}

fun getStatusText(status: LotacaoStatus) = when(status) {
    LotacaoStatus.VAZIO -> "Tranquilo"
    LotacaoStatus.IDEAL -> "Movimentado"
    LotacaoStatus.LOTADO -> "Muito Cheio"
}
