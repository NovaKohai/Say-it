package com.example.sayit

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.sayit.data.local.SayItDatabase
import com.example.sayit.data.repository.CategoryRepositoryImpl
import com.example.sayit.data.repository.TransactionRepositoryImpl
import com.example.sayit.presentation.common.PrivacyDisclosureDialog
import com.example.sayit.presentation.dashboard.DashboardViewModel
import com.example.sayit.presentation.dashboard.DashboardViewModelFactory
import com.example.sayit.presentation.main.MainFintechScreen
import com.example.sayit.theme.SayItTheme

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import com.example.sayit.core.localization.AppLanguage
import com.example.sayit.core.localization.ArabicStrings
import com.example.sayit.core.localization.EnglishStrings
import com.example.sayit.core.localization.LocalStrings
import com.example.sayit.presentation.splash.SplashScreen

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = SayItDatabase.getInstance(applicationContext)
        val txRepo = TransactionRepositoryImpl(db)
        val catRepo = CategoryRepositoryImpl(db)
        val inspectorRepo = com.example.sayit.data.repository.DatabaseInspectorRepositoryImpl(db)
        val prefs = com.example.sayit.data.local.SayItPreferences(applicationContext)
        val factory = DashboardViewModelFactory(txRepo, catRepo, inspectorRepo, prefs)
        val viewModel: DashboardViewModel by viewModels { factory }

        val alreadyAccepted = prefs.isPrivacyDisclosureAccepted

        if (alreadyAccepted) {
            requestAppPermissions()
        }

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val strings = if (uiState.language == AppLanguage.EN) EnglishStrings else ArabicStrings
            val layoutDir = uiState.language.layoutDirection

            CompositionLocalProvider(
                LocalStrings provides strings,
                LocalLayoutDirection provides layoutDir
            ) {
                SayItTheme(darkTheme = uiState.isDarkMode) {
                    var showDisclosure by remember { mutableStateOf(!alreadyAccepted) }
                    var showSplash by remember { mutableStateOf(true) }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            MainFintechScreen(viewModel = viewModel)

                            if (showDisclosure && !showSplash) {
                                PrivacyDisclosureDialog(
                                    onAccept = {
                                        prefs.isPrivacyDisclosureAccepted = true
                                        showDisclosure = false
                                        requestAppPermissions()
                                    },
                                    onDecline = {
                                        showDisclosure = false
                                    }
                                )
                            }

                            AnimatedVisibility(
                                visible = showSplash,
                                enter = androidx.compose.animation.fadeIn(),
                                exit = fadeOut(animationSpec = tween(durationMillis = 400))
                            ) {
                                SplashScreen(
                                    isArabic = uiState.language == AppLanguage.AR,
                                    onSplashFinished = { showSplash = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestAppPermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needed = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            requestPermissionLauncher.launch(needed.toTypedArray())
        }
    }
}
