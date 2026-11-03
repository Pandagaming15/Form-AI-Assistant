package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.formpilot.auth.configureAppCheck
import com.example.formpilot.ui.components.AppDestination
import com.example.formpilot.ui.components.AppHeader
import com.example.formpilot.ui.components.BottomNavBar
import com.example.formpilot.ui.screens.AssistantScreen
import com.example.formpilot.ui.screens.HistoryScreen
import com.example.formpilot.ui.screens.HomeScreen
import com.example.formpilot.ui.screens.LibraryScreen
import com.example.formpilot.ui.screens.WorkspaceScreen
import com.example.ui.theme.FormPilotTheme
import com.example.formpilot.ui.viewmodel.FormPilotViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Mandatory: Configure App Check with launch intent before any UI or Firebase operations
    configureAppCheck(this)

    enableEdgeToEdge()

    setContent {
      FormPilotTheme {
        FormPilotApp()
      }
    }
  }
}

@Composable
fun FormPilotApp(
  viewModel: FormPilotViewModel = viewModel()
) {
  val currentUser by viewModel.currentUser.collectAsState()
  val isAuthLoading by viewModel.isAuthLoading.collectAsState()

  var currentRoute by remember { mutableStateOf(AppDestination.HOME.route) }

  // Android Back Button handling: return to Home screen if on secondary tab
  BackHandler(enabled = currentRoute != AppDestination.HOME.route) {
    currentRoute = AppDestination.HOME.route
  }

  Scaffold(
    topBar = {
      AppHeader(
        currentUser = currentUser,
        authManager = viewModel.authManager,
        isAuthLoading = isAuthLoading
      )
    },
    bottomBar = {
      BottomNavBar(
        currentRoute = currentRoute,
        onNavigate = { route -> currentRoute = route }
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (currentRoute) {
        AppDestination.HOME.route -> {
          HomeScreen(
            viewModel = viewModel,
            onNavigateToWorkspace = { currentRoute = AppDestination.WORKSPACE.route },
            onNavigateToAssistant = { currentRoute = AppDestination.ASSISTANT.route },
            onNavigateToHistory = { currentRoute = AppDestination.HISTORY.route }
          )
        }
        AppDestination.WORKSPACE.route -> {
          WorkspaceScreen(viewModel = viewModel)
        }
        AppDestination.ASSISTANT.route -> {
          AssistantScreen(
            viewModel = viewModel,
            onNavigateToWorkspace = { currentRoute = AppDestination.WORKSPACE.route }
          )
        }
        AppDestination.LIBRARY.route -> {
          LibraryScreen(viewModel = viewModel)
        }
        AppDestination.HISTORY.route -> {
          HistoryScreen(
            viewModel = viewModel,
            onNavigateToWorkspace = { currentRoute = AppDestination.WORKSPACE.route }
          )
        }
        else -> {
          HomeScreen(
            viewModel = viewModel,
            onNavigateToWorkspace = { currentRoute = AppDestination.WORKSPACE.route },
            onNavigateToAssistant = { currentRoute = AppDestination.ASSISTANT.route },
            onNavigateToHistory = { currentRoute = AppDestination.HISTORY.route }
          )
        }
      }
    }
  }
}
