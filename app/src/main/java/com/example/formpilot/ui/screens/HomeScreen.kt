package com.example.formpilot.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.formpilot.model.AnswerMode
import com.example.formpilot.service.GoogleFormsBridge
import com.example.formpilot.ui.viewmodel.FormPilotViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
  viewModel: FormPilotViewModel,
  onNavigateToWorkspace: () -> Unit,
  onNavigateToAssistant: () -> Unit,
  onNavigateToHistory: () -> Unit
) {
  val context = LocalContext.current
  val formUrl by viewModel.formUrl.collectAsState()
  val formTitle by viewModel.formTitle.collectAsState()
  val questions by viewModel.questions.collectAsState()
  val pageState by viewModel.pageState.collectAsState()
  val selectedMode by viewModel.selectedMode.collectAsState()
  val userProfile by viewModel.userProfile.collectAsState()
  val localDrafts by viewModel.localDrafts.collectAsState()
  val cloudDrafts by viewModel.cloudDrafts.collectAsState()

  var inputUrl by remember(formUrl) { mutableStateOf(formUrl) }
  var urlError by remember { mutableStateOf<String?>(null) }

  val answeredCount = questions.count { it.suggestedAnswer.isNotBlank() || it.selectedChoices.isNotEmpty() }
  val totalQuestions = questions.size
  val progress = if (totalQuestions > 0) answeredCount.toFloat() / totalQuestions else 0f

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(8.dp))

      // Hero Welcome Banner
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(
            Brush.linearGradient(
              colors = listOf(
                Color(0xFF0F172A),
                Color(0xFF1E293B),
                Color(0xFF0F2647)
              )
            )
          )
          .padding(20.dp)
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "FormPilot AI",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
              )
              Text(
                text = "Intelligent Google Forms Copilot",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
              )
            }

            Surface(
              color = Color(0xFF38BDF8).copy(alpha = 0.2f),
              shape = RoundedCornerShape(12.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                Text("Gemini 3.8", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF38BDF8))
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Active session summary bar
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFF0F172A).copy(alpha = 0.6f))
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = formTitle,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFFF8FAFC),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "${pageState.pageIndicatorText} • $answeredCount of $totalQuestions questions prepared",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF38BDF8)
              )
              Spacer(modifier = Modifier.height(6.dp))
              LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF38BDF8),
                trackColor = Color(0xFF334155)
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
              onClick = onNavigateToWorkspace,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("resume_workspace_btn")
            ) {
              Text("Open", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // Google Forms URL Input Card
    item {
      ElevatedCard(
        modifier = Modifier.fillMaxWidth().testTag("form_url_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Open Google Form",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Paste a forms.google.com or forms.gle URL to analyze questions",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
            value = inputUrl,
            onValueChange = {
              inputUrl = it
              urlError = null
            },
            modifier = Modifier.fillMaxWidth().testTag("form_url_input"),
            label = { Text("Google Forms URL") },
            placeholder = { Text("https://docs.google.com/forms/d/e/...") },
            singleLine = true,
            trailingIcon = {
              IconButton(
                onClick = {
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  val clip = clipboard.primaryClip
                  if (clip != null && clip.itemCount > 0) {
                    val pasted = clip.getItemAt(0).text?.toString() ?: ""
                    inputUrl = pasted
                  }
                },
                modifier = Modifier.testTag("paste_url_btn")
              ) {
                Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = MaterialTheme.colorScheme.primary)
              }
            },
            isError = urlError != null,
            supportingText = urlError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
          )

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = {
                if (inputUrl.isBlank()) {
                  urlError = "Please enter a Google Forms URL."
                  return@Button
                }
                viewModel.setFormUrl(inputUrl)
                viewModel.setWorkspaceTab(0) // Live Web View tab
                onNavigateToWorkspace()
              },
              modifier = Modifier.weight(1f).testTag("open_form_btn"),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
              Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Open Form")
            }

            OutlinedButton(
              onClick = {
                viewModel.loadSampleDemoForm()
                viewModel.setWorkspaceTab(1) // AI Answers tab
                onNavigateToWorkspace()
              },
              modifier = Modifier.testTag("load_demo_form_btn")
            ) {
              Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Try Demo")
            }
          }
        }
      }
    }

    // AI Mode Selector
    item {
      Column {
        Text(
          text = "AI Answering Mode",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Customize the voice, depth, and tone of generated answers",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          AnswerMode.entries.forEach { mode ->
            val isSelected = selectedMode == mode
            FilterChip(
              selected = isSelected,
              onClick = { viewModel.setAnswerMode(mode) },
              label = { Text(mode.title) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
              ),
              modifier = Modifier.testTag("mode_chip_${mode.name}")
            )
          }
        }
      }
    }

    // Quick Capabilities Cards
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Card(
          modifier = Modifier
            .weight(1f)
            .clickable { onNavigateToAssistant() }
            .testTag("quick_screenshot_card"),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Box(
              modifier = Modifier.size(32.dp).background(Color(0xFF818CF8).copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Vision OCR", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Text("Upload screenshot of questions", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }

        Card(
          modifier = Modifier
            .weight(1f)
            .clickable { onNavigateToWorkspace() }
            .testTag("quick_autofill_card"),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Box(
              modifier = Modifier.size(32.dp).background(Color(0xFF10B981).copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Auto-Fill Bridge", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Text("Insert into live form fields", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    }

    // Recent Drafts Preview
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Saved Drafts & Sessions",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )

        IconButton(onClick = onNavigateToHistory) {
          Icon(Icons.Default.History, contentDescription = "View all history", tint = MaterialTheme.colorScheme.primary)
        }
      }
    }

    if (localDrafts.isEmpty() && cloudDrafts.isEmpty()) {
      item {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("No saved drafts yet", style = MaterialTheme.typography.titleSmall)
            Text("Forms you open and answer will automatically save here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    } else {
      items(localDrafts.take(3)) { draft ->
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              viewModel.resumeLocalDraft(draft)
              onNavigateToWorkspace()
            },
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = draft.formTitle,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = draft.formUrl,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Surface(
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text("Resume", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
