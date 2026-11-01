package com.example.formpilot.ui.components

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

enum class AppDestination(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector
) {
  HOME("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
  WORKSPACE("workspace", "Form", Icons.Filled.Web, Icons.Outlined.Web),
  ASSISTANT("assistant", "AI Pilot", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
  LIBRARY("library", "Library", Icons.Filled.Bookmarks, Icons.Outlined.Bookmarks),
  HISTORY("history", "History", Icons.Filled.History, Icons.Outlined.History)
}

@Composable
fun BottomNavBar(
  currentRoute: String,
  onNavigate: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  NavigationBar(
    modifier = modifier
      .navigationBarsPadding()
      .testTag("bottom_nav_bar"),
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 8.dp
  ) {
    AppDestination.entries.forEach { destination ->
      val isSelected = currentRoute == destination.route
      NavigationBarItem(
        selected = isSelected,
        onClick = { onNavigate(destination.route) },
        icon = {
          Icon(
            imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
            contentDescription = destination.label
          )
        },
        label = { Text(destination.label) },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = MaterialTheme.colorScheme.onPrimary,
          selectedTextColor = MaterialTheme.colorScheme.primary,
          indicatorColor = MaterialTheme.colorScheme.primary,
          unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
          unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.testTag("nav_item_${destination.route}")
      )
    }
  }
}
