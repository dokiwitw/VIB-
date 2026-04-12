package com.mediquest.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediquest.app.data.*
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(vm: VIBViewModel, onBack: () -> Unit) {
    val user by vm.user.collectAsState()
    val context = LocalContext.current
    
    val userBitmap = remember(user.photoBase64) {
        user.photoBase64?.let { base64 ->
            try {
                val decodedString = Base64.decode(base64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
            } catch (e: Exception) { null }
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            
            // Redimensiona para economizar banco de dados (Max 200x200)
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
            
            vm.updateProfilePhoto(base64)
        }
    }

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
            // Avatar com Foto Real
            Box(
                Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { photoLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (userBitmap != null) {
                    Image(
                        bitmap = userBitmap.asImageBitmap(),
                        contentDescription = "Foto de Perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(32.dp))
                        Text("Add Foto", style = MaterialTheme.typography.labelSmall)
                    }
                }
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

            // Ghost Mode Switch
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (user.isGhost) MaterialTheme.colorScheme.errorContainer.copy(0.3f) 
                                     else MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)
                )
            ) {
                Row(
                    Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("👻 Ghost Mode", fontWeight = FontWeight.Bold)
                        Text(
                            if (user.isGhost) "Você está invisível no mapa." 
                            else "Sua localização está visível para outros.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = user.isGhost,
                        onCheckedChange = { vm.toggleGhostMode(it) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // VIB! Pass Card
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (user.hasVibPass) Color(0xFFFFD700).copy(0.15f) 
                                     else MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)
                )
            ) {
                Row(
                    Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎫 VIB! Pass", fontWeight = FontWeight.Bold)
                            if (user.hasVibPass) {
                                Spacer(Modifier.width(8.dp))
                                Surface(color = Color(0xFFFFD700), shape = CircleShape) {
                                    Text("ATIVO", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), 
                                         style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color.Black)
                                }
                            }
                        }
                        Text(
                            if (user.hasVibPass) "Benefícios exclusivos liberados!" 
                            else "Assine para ganhar drinks e descontos.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = user.hasVibPass,
                        onCheckedChange = { vm.toggleVibPass(it) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

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
