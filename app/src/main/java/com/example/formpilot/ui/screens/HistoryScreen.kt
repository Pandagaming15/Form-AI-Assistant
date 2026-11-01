package com.example.formpilot.ui.screens

import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.formpilot.ui.viewmodel.FormPilotViewModel

@Composable
fun HistoryScreen(
  viewModel: FormPilotViewModel,
  onNavigateToWorkspace: () -> Unit
) {
  val context = LocalContext.current
  val localDrafts by viewModel.localDrafts.collectAsState()
  val cloudDrafts by viewModel.cloudDrafts.collectAsState()
  val currentUser by viewModel.currentUser.collectAsState()

  var selectedTab by remember { mutableStateOf(0) } // 0: Local Device Drafts, 1: Cloud Sessions

  Column(modifier = Modifier.fillMaxSize()) {
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp
    ) {
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
              Text("On-Device Drafts (${localDrafts.size})")
            }
          },
          modifier = Modifier.testTag("tab_local_drafts")
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp))
              Text("Cloud Sessions (${cloudDrafts.size})")
            }
          },
          modifier = Modifier.testTag("tab_cloud_drafts")
        )
      }
    }

    when (selectedTab) {
      0 -> {
        // Local Device Drafts
        if (localDrafts.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
              Text("No on-device drafts found", style = MaterialTheme.typography.titleMedium)
              Text("Drafts save automatically as you edit forms.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            items(localDrafts, key = { it.id }) { draft ->
              val timeAgo = DateUtils.getRelativeTimeSpanString(draft.updatedAt)
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    viewModel.resumeLocalDraft(draft)
                    onNavigateToWorkspace()
                  }
                  .testTag("draft_item_${draft.id}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = draft.formTitle,
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.weight(1f),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    IconButton(
                      onClick = {
                        viewModel.deleteLocalDraft(draft.id)
                        Toast.makeText(context, "Draft deleted", Toast.LENGTH_SHORT).show()
                      },
                      modifier = Modifier.size(28.dp)
                    ) {
                      Icon(Icons.Default.Delete, contentDescription = "Delete draft", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                  }

                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = draft.formUrl,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )

                  Spacer(modifier = Modifier.height(10.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "Last updated $timeAgo",
                      style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                      onClick = {
                        viewModel.resumeLocalDraft(draft)
                        onNavigateToWorkspace()
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.height(32.dp)
                    ) {
                      Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Resume", style = MaterialTheme.typography.labelSmall)
                    }
                  }
                }
              }
            }
          }
        }
      }

      1 -> {
        // Cloud Sync Sessions
        if (currentUser == null) {
          Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Icon(Icons.Default.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
              Text("Sign in to View Cloud Drafts", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
              Text("Cloud synchronization keeps your forms available across all your Android devices.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        } else if (cloudDrafts.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No cloud sessions yet. Active forms sync automatically.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            items(cloudDrafts, key = { it.id }) { session ->
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    viewModel.resumeCloudDraft(session)
                    onNavigateToWorkspace()
                  },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = session.formTitle,
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                      modifier = Modifier.weight(1f),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    IconButton(
                      onClick = {
                        viewModel.deleteCloudDraft(session.id)
                        Toast.makeText(context, "Cloud session deleted", Toast.LENGTH_SHORT).show()
                      },
                      modifier = Modifier.size(28.dp)
                    ) {
                      Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                  }

                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "${session.completedQuestionsCount} of ${session.detectedQuestionsCount} questions answered",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                  )

                  Spacer(modifier = Modifier.height(10.dp))

                  Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Button(
                      onClick = {
                        viewModel.resumeCloudDraft(session)
                        onNavigateToWorkspace()
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.height(32.dp)
                    ) {
                      Text("Resume Cloud Draft", style = MaterialTheme.typography.labelSmall)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
