package com.tabbify.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.tabbify.ui.home.HomeScreen
import com.tabbify.ui.theme.TabbifyTheme

@Composable
fun TabbifyRootApp() {
    TabbifyTheme(darkTheme = true) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Navigator(HomeScreen()) { navigator ->
                SlideTransition(navigator)
            }
        }
    }
}
