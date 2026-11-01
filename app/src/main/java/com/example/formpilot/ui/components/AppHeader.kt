package com.example.formpilot.ui.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.formpilot.auth.AuthManager
import com.google.firebase.auth.FirebaseUser

@Composable
fun AppHeader(
  currentUser: FirebaseUser?,
  authManager: AuthManager,
  isAuthLoading: Boolean
) {
  val context = LocalContext.current
  val activity = context as? Activity
  val scope = rememberCoroutineScope()
  var showAccountDialog by remember { mutableStateOf(false) }

  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 4.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // App Branding
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .background(
              Brush.linearGradient(
                colors = listOf(Color(0xFF38BDF8), Color(0xFF818CF8))
              ),
              shape = RoundedCornerShape(10.dp)
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "FormPilot Logo",
            tint = Color(0xFF0F172A),
            modifier = Modifier.size(22.dp)
          )
        }
        Column {
          Text(
            text = "FormPilot AI",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Smart Google Forms Assistant",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // User Account Pill
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .clip(RoundedCornerShape(20.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant)
          .border(
            width = 1.dp,
            color = if (currentUser != null) Color(0xFF10B981) else MaterialTheme.colorScheme.outline,
            shape = RoundedCornerShape(20.dp)
          )
          .clickable { showAccountDialog = true }
          .padding(horizontal = 10.dp, vertical = 6.dp)
          .testTag("account_pill_button"),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        if (currentUser?.photoUrl != null) {
          AsyncImage(
            model = currentUser.photoUrl,
            contentDescription = "User Avatar",
            modifier = Modifier
              .size(24.dp)
              .clip(CircleShape)
          )
        } else {
          Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Account",
            modifier = Modifier.size(24.dp),
            tint = if (currentUser != null) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Column {
          Text(
            text = currentUser?.displayName ?: "Sign In",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Box(
          modifier = Modifier
            .size(8.dp)
            .background(
              color = if (currentUser != null) Color(0xFF10B981) else Color(0xFFF59E0B),
              shape = CircleShape
            )
        )
      }
    }
  }

  // Account Details & Clarification Modal
  if (showAccountDialog) {
    AlertDialog(
      onDismissRequest = { showAccountDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Text("Google Account Identity")
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          if (currentUser != null) {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Text(
                  text = "Signed in as:",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = currentUser.displayName ?: "Google User",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = currentUser.email ?: "",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                  Text("Account Status: Connected", style = MaterialTheme.typography.labelSmall, color = Color(0xFF10B981))
                }
              }
            }
          } else {
            Text(
              "You are currently using FormPilot AI in guest mode. Sign in with your Google account to unlock cloud drafts, personal memory, and synchronized answer library.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          // Distinct Sessions Transparency Notice
          Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.Top
            ) {
              Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
              Column {
                Text(
                  "Session Identity Notice",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = Color(0xFF38BDF8)
                )
                Text(
                  "1. FormPilot AI Account: Used for Gemini AI answering and cloud drafts.\n" +
                  "2. Google Forms Browser Session: When submitting restricted forms, Google verifies the account authenticated in the browser or WebView.",
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }

          if (isAuthLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
          }
        }
      },
      confirmButton = {
        if (currentUser == null) {
          Button(
            onClick = {
              activity?.let {
                authManager.signInWithGoogle(it, scope) {
                  showAccountDialog = false
                }
              }
            },
            modifier = Modifier.testTag("dialog_google_signin_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
          ) {
            Text("Sign In with Google")
          }
        } else {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = {
                activity?.let {
                  authManager.signInWithGoogle(it, scope)
                }
              },
              modifier = Modifier.testTag("switch_account_button")
            ) {
              Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Switch")
            }
            Button(
              onClick = {
                authManager.signOut(scope) {
                  showAccountDialog = false
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
              modifier = Modifier.testTag("signout_button")
            ) {
              Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Sign Out")
            }
          }
        }
      },
      dismissButton = {
        TextButton(onClick = { showAccountDialog = false }) {
          Text("Close")
        }
      }
    )
  }
}
