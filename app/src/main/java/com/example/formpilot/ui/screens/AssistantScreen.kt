package com.example.formpilot.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.formpilot.ui.viewmodel.FormPilotViewModel

@Composable
fun AssistantScreen(
  viewModel: FormPilotViewModel,
  onNavigateToWorkspace: () -> Unit
) {
  val context = LocalContext.current
  val customInstructions by viewModel.customInstructions.collectAsState()
  val answerLength by viewModel.answerLength.collectAsState()
  val referenceDocs by viewModel.referenceDocs.collectAsState()
  val isGenerating by viewModel.isGenerating.collectAsState()

  var pastedText by remember { mutableStateOf("") }
  var newDocTitle by remember { mutableStateOf("") }
  var newDocContent by remember { mutableStateOf("") }
  var showAddDocCard by remember { mutableStateOf(false) }

  // Zero-permission Android Photo Picker for multimodal screenshot question recognition
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    uri?.let {
      try {
        val inputStream = context.contentResolver.openInputStream(it)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        if (bitmap != null) {
          viewModel.extractFromScreenshot(bitmap)
          Toast.makeText(context, "Analyzing form screenshot with Gemini Vision...", Toast.LENGTH_SHORT).show()
          onNavigateToWorkspace()
        }
      } catch (e: Exception) {
        Toast.makeText(context, "Failed to decode image: ${e.message}", Toast.LENGTH_SHORT).show()
      }
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header Info
    item {
      Column {
        Text(
          text = "AI Form Copilot",
          style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Multimodal OCR, custom reasoning, and reference knowledge grounding",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    // 1. Screenshot Question Recognizer Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth().testTag("screenshot_scanner_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
              text = "Screenshot Question Reader",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Take or upload a screenshot of any Google Form. Gemini Vision will read and extract the questions, types, and choices automatically.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(12.dp))

          Button(
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            modifier = Modifier.fillMaxWidth().testTag("pick_screenshot_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
          ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Select Form Screenshot")
          }
        }
      }
    }

    // 2. Paste Copied Questions / Form Text Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth().testTag("paste_questions_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.ContentPaste, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Text(
              text = "Paste Raw Questions Text",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Have questions copied from a PDF, document, or form page? Paste them here and AI will structure them into interactive fields.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(10.dp))

          OutlinedTextField(
            value = pastedText,
            onValueChange = { pastedText = it },
            modifier = Modifier.fillMaxWidth().testTag("raw_text_input"),
            placeholder = { Text("Paste questions or raw form text here...") },
            minLines = 3,
            maxLines = 6
          )

          Spacer(modifier = Modifier.height(10.dp))

          Button(
            onClick = {
              if (pastedText.isNotBlank()) {
                viewModel.extractFromPastedText(pastedText)
                onNavigateToWorkspace()
              }
            },
            enabled = pastedText.isNotBlank() && !isGenerating,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.fillMaxWidth().testTag("parse_pasted_btn")
          ) {
            Text("Parse Into Form")
          }
        }
      }
    }

    // 3. Custom Answering Instructions & Length Control
    item {
      Card(
        modifier = Modifier.fillMaxWidth().testTag("custom_instructions_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Custom Instructions & Length",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Fine-tune answer length and specify specialized rules (e.g., 'Answer concisely', 'Use first-person voice').",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(10.dp))

          // Answer Length Chips
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("concise", "standard", "detailed").forEach { len ->
              val isSel = answerLength == len
              FilterChip(
                selected = isSel,
                onClick = { viewModel.setAnswerLength(len) },
                label = { Text(len.replaceFirstChar { it.uppercase() }) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primary,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          OutlinedTextField(
            value = customInstructions,
            onValueChange = { viewModel.setCustomInstructions(it) },
            modifier = Modifier.fillMaxWidth().testTag("custom_instructions_input"),
            label = { Text("Special Prompting Rules") },
            placeholder = { Text("e.g. Keep answers under 40 words, use authentic student perspective") },
            minLines = 2
          )
        }
      }
    }

    // 4. Reference Materials & Source Documents
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Reference Documents",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        IconButton(onClick = { showAddDocCard = !showAddDocCard }) {
          Icon(Icons.Default.Add, contentDescription = "Add Reference Document", tint = MaterialTheme.colorScheme.primary)
        }
      }
    }

    if (showAddDocCard) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Add Reference Material", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            OutlinedTextField(
              value = newDocTitle,
              onValueChange = { newDocTitle = it },
              label = { Text("Document Title (e.g. Resume_Notes.txt)") },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true
            )
            OutlinedTextField(
              value = newDocContent,
              onValueChange = { newDocContent = it },
              label = { Text("Document Text / Background Info") },
              modifier = Modifier.fillMaxWidth(),
              minLines = 3
            )
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
              OutlinedButton(onClick = { showAddDocCard = false }) {
                Text("Cancel")
              }
              Spacer(modifier = Modifier.width(8.dp))
              Button(
                onClick = {
                  if (newDocTitle.isNotBlank() && newDocContent.isNotBlank()) {
                    viewModel.addReferenceDocument(newDocTitle, newDocContent)
                    newDocTitle = ""
                    newDocContent = ""
                    showAddDocCard = false
                  }
                }
              ) {
                Text("Add")
              }
            }
          }
        }
      }
    }

    items(referenceDocs) { doc ->
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Checkbox(
              checked = doc.isSelected,
              onCheckedChange = { viewModel.toggleReferenceDocSelection(doc.id) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = doc.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
              )
              Text(
                text = "${doc.sizeKb} KB • ${if (doc.isSelected) "Active in AI prompts" else "Disabled"}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = if (doc.isSelected) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          IconButton(onClick = { viewModel.deleteReferenceDocument(doc.id) }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
