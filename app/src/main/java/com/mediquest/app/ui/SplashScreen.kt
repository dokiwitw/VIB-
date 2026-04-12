package com.mediquest.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Anima entrada do logo
        scale.animateTo(1f, animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow
        ))
        alpha.animateTo(1f, tween(400))
        delay(1200)
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F1A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale.value)
        ) {
            Text("🗺️", fontSize = 72.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "HangSpot",
                fontSize   = 36.sp,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFFC4B5FD)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "O Waze das baladas",
                fontSize  = 16.sp,
                color     = Color(0xFF9CA3AF),
                textAlign = TextAlign.Center
            )
        }
    }
}
