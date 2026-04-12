package com.mediquest.app.ui

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediquest.app.viewmodel.MediQuestViewModel

sealed class Screen {
    object Splash   : Screen()
    object Map      : Screen()
    object Ranking  : Screen()
    object Profile  : Screen()
}

@Composable
fun AppNavigation(vm: MediQuestViewModel = viewModel()) {
    var current by remember { mutableStateOf<Screen>(Screen.Splash) }

    when (current) {
        is Screen.Splash  -> SplashScreen(onFinished = { current = Screen.Map })
        is Screen.Map     -> MapScreen(
            vm        = vm,
            onRanking = { current = Screen.Ranking },
            onProfile = { current = Screen.Profile }
        )
        is Screen.Ranking -> RankingScreen(
            vm           = vm,
            onBack       = { current = Screen.Map }
        )
        is Screen.Profile -> ProfileScreen(
            vm     = vm,
            onBack = { current = Screen.Map }
        )
    }
}
