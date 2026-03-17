package com.iceman.teveclub.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iceman.teveclub.TeveViewModel

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val vm: TeveViewModel = viewModel()
    val isLoggedIn = vm.isLoggedIn.collectAsState()

    if (isLoggedIn.value) {
        DashboardScreen(vm = vm)
    } else {
        LoginScreen(vm = vm)
    }
}
