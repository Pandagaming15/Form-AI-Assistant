package com.example.formpilot.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.formpilot.model.UserProfile
import com.example.formpilot.ui.viewmodel.FormPilotViewModel

@Composable
fun LibraryScreen(
  viewModel: FormPilotViewModel
) {
  val context = LocalContext.current
  val templates by viewModel.templates.collectAsState()
  val userProfile by viewModel.userProfile.collectAsState()
  val currentUser by viewModel.currentUser.collectAsState()

  var selectedTab by remember { mutableStateOf(0) } // 0: Answer Templates, 1: Profile & Memory

  // Template creation dialog state
  var showAddTemplate by remember { mutableStateOf(false) }
  var templateTitle by remember { mutableStateOf("") }
  var templateCategory by remember { mutableStateOf("Personal") }
  var templateContent by remember { mutableStateOf("") }

  // Profile editing state
  var name by remember(userProfile) { mutableStateOf(userProfile?.displayName ?: "") }
  var school by remember(userProfile) { mutableStateOf(userProfile?.schoolOrOrg ?: "") }
  var dept by remember(userProfile) { mutableStateOf(userProfile?.department ?: "") }
  var notes by remember(userProfile) { mutableStateOf(userProfile?.notes ?: "") }
  var memoryEnabled by remember(userProfile) { mutableStateOf(userProfile?.memoryEnabled ?: true) }

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
          text = { Text("Answer Library", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
          modifier = Modifier.testTag("tab_answer_library")
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("Personal Memory", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
          modifier = Modifier.testTag("tab_profile_memory")
        )
      }
    }

    when (selectedTab) {
      0 -> {
        // Answer Library List
        LazyColumn(
          modifier = Modifier.fillMaxSize().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "Reusable Response Library",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                  text = "Approved answer templates for recurring form questions",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              IconButton(onClick = { showAddTemplate = !showAddTemplate }) {
                Icon(Icons.Default.Add, contentDescription = "Add template", tint = MaterialTheme.colorScheme.primary)
              }
            }
          }

          if (showAddTemplate) {
            item {
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  Text("New Answer Template", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                  OutlinedTextField(
                    value = templateTitle,
                    onValueChange = { templateTitle = it },
                    label = { Text("Template Title (e.g. Work Experience Summary)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                  )
                  OutlinedTextField(
                    value = templateCategory,
                    onValueChange = { templateCategory = it },
                    label = { Text("Category (e.g. Work, Academic, Personal)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                  )
                  OutlinedTextField(
                    value = templateContent,
                    onValueChange = { templateContent = it },
                    label = { Text("Template Content") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                  )
                  Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { showAddTemplate = false }) {
                      Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                      onClick = {
                        if (templateTitle.isNotBlank() && templateContent.isNotBlank()) {
                          viewModel.addTemplate(templateTitle, templateCategory, templateContent, listOf(templateCategory.lowercase()))
                          templateTitle = ""
                          templateContent = ""
                          showAddTemplate = false
                          Toast.makeText(context, "Template saved to library", Toast.LENGTH_SHORT).show()
                        }
                      }
                    ) {
                      Text("Save Template")
                    }
                  }
                }
              }
            }
          }

          items(templates) { template ->
            Card(
              modifier = Modifier.fillMaxWidth().testTag("template_card_${template.id}"),
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                      color = MaterialTheme.colorScheme.primaryContainer,
                      shape = RoundedCornerShape(6.dp)
                    ) {
                      Text(
                        text = template.category,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                      )
                    }
                    Text(
                      text = template.title,
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onSurface
                    )
                  }

                  Row {
                    IconButton(
                      onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Template", template.content)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Template copied!", Toast.LENGTH_SHORT).show()
                      },
                      modifier = Modifier.size(32.dp)
                    ) {
                      Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                      onClick = { viewModel.deleteTemplate(template.id) },
                      modifier = Modifier.size(32.dp)
                    ) {
                      Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                  color = MaterialTheme.colorScheme.surfaceVariant,
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    text = template.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(10.dp)
                  )
                }
              }
            }
          }
        }
      }

      1 -> {
        // Personal Memory Settings
        LazyColumn(
          modifier = Modifier.fillMaxSize().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          item {
            Card(
              modifier = Modifier.fillMaxWidth().testTag("profile_memory_card"),
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Apply Personal Memory", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                      "Allow AI to use this non-sensitive profile info to fill personal questions accurately.",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  Switch(
                    checked = memoryEnabled,
                    onCheckedChange = { memoryEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                  )
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                  value = name,
                  onValueChange = { name = it },
                  label = { Text("Preferred Full Name") },
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true
                )

                OutlinedTextField(
                  value = school,
                  onValueChange = { school = it },
                  label = { Text("School / College / Organization") },
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true
                )

                OutlinedTextField(
                  value = dept,
                  onValueChange = { dept = it },
                  label = { Text("Department / Major / Course") },
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true
                )

                OutlinedTextField(
                  value = notes,
                  onValueChange = { notes = it },
                  label = { Text("Personal Context & Academic Bio") },
                  placeholder = { Text("e.g. 3rd-year student, specializes in software engineering, available for summer internships.") },
                  modifier = Modifier.fillMaxWidth(),
                  minLines = 3
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                  onClick = {
                    val updated = UserProfile(
                      userId = currentUser?.uid ?: "",
                      displayName = name,
                      email = currentUser?.email ?: "",
                      schoolOrOrg = school,
                      department = dept,
                      notes = notes,
                      memoryEnabled = memoryEnabled
                    )
                    viewModel.updateProfile(updated)
                    Toast.makeText(context, "Profile memory saved!", Toast.LENGTH_SHORT).show()
                  },
                  modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_profile_btn"),
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                  Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Save Profile & Preferences")
                }
              }
            }
          }

          // Privacy Notice
          item {
            Surface(
              color = Color(0xFF1E293B),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Text("Privacy & Security Guarantee", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF38BDF8))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  "• Only explicitly provided details are stored.\n" +
                  "• Passwords and sensitive authentication tokens are NEVER collected or stored.\n" +
                  "• You can clear your profile or disable memory at any time.",
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                  color = Color(0xFF94A3B8)
                )
              }
            }
          }
        }
      }
    }
  }
}
