package com.app.biashara.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.app.biashara.presentation.viewmodel.KmpViewModel
import org.koin.compose.koinInject

/**
 * Resolves a KMP ViewModel from Koin, remembers it across recompositions,
 * and automatically disposes of its coroutine scope when leaving the composition.
 */
@Composable
inline fun <reified T : KmpViewModel> kmpViewModel(): T {
    val viewModel = koinInject<T>()
    val rememberedViewModel = remember { viewModel }
    
    DisposableEffect(rememberedViewModel) {
        onDispose {
            rememberedViewModel.clear()
        }
    }
    
    return rememberedViewModel
}
