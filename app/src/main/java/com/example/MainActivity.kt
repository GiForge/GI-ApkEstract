package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.screens.MainScreen
import com.example.ui.theme.ApkExtractorTheme
import com.example.viewmodel.ApkViewModel

class MainActivity : ComponentActivity() {

    private val apkViewModel: ApkViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ApkExtractorTheme {
                MainScreen(viewModel = apkViewModel)
            }
        }
    }
}
