package com.mediquest.app.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import com.google.maps.android.heatmaps.Gradient
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.WeightedLatLng
import com.mediquest.app.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    vm: VIBViewModel = viewModel(),
    onRanking: () -> Unit,
    onProfile: () -> Unit
) {
    val hotspots by vm.hotspots.collectAsState()
    val snackbarMsg by vm.snackbarMensagem.collectAsState()
    val user by vm.user.collectAsState()
    val loading by vm.loading.collectAsState()
    val filtroAtivo by vm.filtroAtivo.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedHotspot by remember { mutableStateOf<Hotspot?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            vm.limparSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("VIB!", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                },
                navigationIcon = {
                    // Substituída a Estrela pelo Seletor de Filtros
                    IconButton(onClick = { showFilterSheet = true }) {
                        BadgedBox(
                            badge = {
                                if (filtroAtivo != null) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary)
                                }
                            }
                        ) {
                            Icon(Icons.Filled.FilterList, "Filtros")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onProfile) {
                        Icon(Icons.Filled.Person, "Perfil")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { vm.buscarTudo() },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                else Icon(Icons.Filled.Refresh, "Atualizar")
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(LatLng(-15.793889, -47.882778), 13f)
            }

            // O peso agora é a combinação da Popularidade Real (Places) com a Lotação Atual
            val spotData = remember(hotspots) {
                hotspots.filter { it.categoria != CategoriaLocal.PARQUE }.map { h ->
                    val fatorLotacao = when (h.lotacaoAtual) {
                        LotacaoStatus.VAZIO -> 0.1
                        LotacaoStatus.IDEAL -> 0.6
                        LotacaoStatus.LOTADO -> 1.0
                    }
                    // Peso final = Popularidade do Local (0.1 a 1.0) * Ocupação (0.1 a 1.0)
                    val finalWeight = (h.pesoAgregado * fatorLotacao).coerceIn(0.01, 1.0)
                    WeightedLatLng(LatLng(h.latitude, h.longitude), finalWeight)
                }
            }

            val parkData = remember(hotspots) {
                hotspots.filter { it.categoria == CategoriaLocal.PARQUE }.map { h ->
                    val fatorLotacao = when (h.lotacaoAtual) {
                        LotacaoStatus.VAZIO -> 0.05
                        LotacaoStatus.IDEAL -> 0.2
                        LotacaoStatus.LOTADO -> 0.4
                    }
                    val finalWeight = (h.pesoAgregado * fatorLotacao).coerceIn(0.01, 1.0)
                    WeightedLatLng(LatLng(h.latitude, h.longitude), finalWeight)
                }
            }

            val spotProvider = remember(spotData) {
                if (spotData.isEmpty()) null
                else HeatmapTileProvider.Builder()
                    .weightedData(spotData)
                    .radius(50) 
                    .opacity(0.7) 
                    .gradient(Gradient(
                        intArrayOf(
                            android.graphics.Color.argb(0, 0, 225, 255),
                            android.graphics.Color.argb(200, 0, 225, 255),
                            android.graphics.Color.argb(200, 0, 255, 0),
                            android.graphics.Color.argb(200, 255, 0, 0)
                        ),
                        floatArrayOf(0.0f, 0.15f, 0.4f, 0.8f) // Puxando as cores para mais perto do centro (unificação)
                    ))
                    .build()
            }

            val parkProvider = remember(parkData) {
                if (parkData.isEmpty()) null
                else HeatmapTileProvider.Builder()
                    .weightedData(parkData)
                    .radius(50)
                    .opacity(0.4) 
                    .gradient(Gradient(
                        intArrayOf(
                            android.graphics.Color.argb(0, 0, 225, 255),
                            android.graphics.Color.argb(150, 0, 225, 255),
                            android.graphics.Color.argb(150, 0, 255, 0),
                            android.graphics.Color.argb(150, 255, 0, 0)
                        ),
                        floatArrayOf(0.0f, 0.15f, 0.4f, 0.8f) // Mesma lógica de unificação
                    ))
                    .build()
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = true),
                properties = MapProperties(
                    isMyLocationEnabled = true,
                    mapStyleOptions = MapStyleOptions(MAP_STYLE_JSON)
                )
            ) {
                spotProvider?.let { TileOverlay(tileProvider = it) }
                parkProvider?.let { TileOverlay(tileProvider = it) }

                hotspots.forEach { hotspot ->
                    Marker(
                        state = MarkerState(position = LatLng(hotspot.latitude, hotspot.longitude)),
                        alpha = 0.0f,
                        onClick = {
                            selectedHotspot = hotspot
                            true
                        }
                    )
                }
            }

            // Legenda
            Card(
                Modifier.align(Alignment.BottomStart).padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(0.9f))
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LegendItem("Muito Cheio", Color.Red)
                    LegendItem("Movimentado", Color.Green)
                    LegendItem("Tranquilo", Color.Cyan)
                }
            }
        }

        // Seletor de Filtros (Bottom Sheet)
        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(Modifier.fillMaxWidth().padding(24.dp)) {
                    Text("FILTRAR CATEGORIAS", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(16.dp))
                    
                    CategoriaLocal.entries.forEach { cat ->
                        val selecionado = filtroAtivo == cat
                        Surface(
                            onClick = { 
                                vm.setFiltro(if (selecionado) null else cat)
                                showFilterSheet = false
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            color = if (selecionado) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            border = if (selecionado) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(0.3f))
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(cat.displayName, fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal)
                                if (selecionado) {
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                    
                    if (filtroAtivo != null) {
                        TextButton(
                            onClick = { vm.setFiltro(null); showFilterSheet = false },
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp)
                        ) {
                            Text("LIMPAR FILTROS", color = Color.Red)
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }

        // Detalhes do Hotspot
        selectedHotspot?.let { hotspot ->
            HotspotDetailSheet(
                hotspot = hotspot,
                onDismiss = { selectedHotspot = null },
                onReporte = { status ->
                    vm.processarReporte(ReporteLotacao(hotspot.id, status, user.id))
                }
            )
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private const val MAP_STYLE_JSON = """
[
  { "elementType": "geometry", "stylers": [ { "color": "#242f3e" } ] },
  { "elementType": "labels.text.stroke", "stylers": [ { "color": "#242f3e" } ] },
  { "elementType": "labels.text.fill", "stylers": [ { "color": "#746855" } ] },
  { "featureType": "administrative.locality", "elementType": "labels.text.fill", "stylers": [ { "color": "#d59563" } ] },
  { "featureType": "poi", "elementType": "labels.text.fill", "stylers": [ { "color": "#d59563" } ] },
  { "featureType": "poi.park", "elementType": "geometry", "stylers": [ { "color": "#263c3f" } ] },
  { "featureType": "poi.park", "elementType": "labels.text.fill", "stylers": [ { "color": "#6b9a76" } ] },
  { "featureType": "road", "elementType": "geometry", "stylers": [ { "color": "#38413e" } ] },
  { "featureType": "road", "elementType": "geometry.stroke", "stylers": [ { "color": "#212a37" } ] },
  { "featureType": "road", "elementType": "labels.text.fill", "stylers": [ { "color": "#9ca5b3" } ] },
  { "featureType": "road.highway", "elementType": "geometry", "stylers": [ { "color": "#746855" } ] },
  { "featureType": "road.highway", "elementType": "geometry.stroke", "stylers": [ { "color": "#1f2835" } ] },
  { "featureType": "road.highway", "elementType": "labels.text.fill", "stylers": [ { "color": "#f3d19c" } ] },
  { "featureType": "transit", "elementType": "geometry", "stylers": [ { "color": "#2f3948" } ] },
  { "featureType": "transit.station", "elementType": "labels.text.fill", "stylers": [ { "color": "#d59563" } ] },
  { "featureType": "water", "elementType": "geometry", "stylers": [ { "color": "#17263c" } ] },
  { "featureType": "water", "elementType": "labels.text.fill", "stylers": [ { "color": "#515c6d" } ] },
  { "featureType": "water", "elementType": "labels.text.stroke", "stylers": [ { "color": "#17263c" } ] }
]
"""
