package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CareerViewModel

class MainActivity : ComponentActivity() {
  private val viewModel: CareerViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val currentAnimeSkin by viewModel.selectedAnimeSkin.collectAsStateWithLifecycle()
      val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
      MyApplicationTheme(animeSkin = currentAnimeSkin, darkTheme = isDarkMode) {
        MainScreen(
          viewModel = viewModel,
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }
}



