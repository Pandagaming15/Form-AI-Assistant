package com.example.formpilot.ui.screens

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.formpilot.model.FormQuestion
import com.example.formpilot.service.GoogleFormsBridge
import com.example.formpilot.ui.components.AnswerCard
import com.example.formpilot.ui.viewmodel.FormPilotViewModel

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
  viewModel: FormPilotViewModel
) {
  val context = LocalContext.current
  val formUrl by viewModel.formUrl.collectAsState()
  val formTitle by viewModel.formTitle.collectAsState()
  val questions by viewModel.questions.collectAsState()
  val isGenerating by viewModel.isGenerating.collectAsState()
  val generationError by viewModel.generationError.collectAsState()
  val fillReport by viewModel.fillReport.collectAsState()
  val consistencyReport by viewModel.consistencyReport.collectAsState()
  val isAnalyzingConsistency by viewModel.isAnalyzingConsistency.collectAsState()
  val selectedTab by viewModel.workspaceTab.collectAsState()

  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  var webLoadingProgress by remember { mutableStateOf(0) }
  var isWebLoading by remember { mutableStateOf(false) }
  var showConsistencyDialog by remember { mutableStateOf(false) }

  val tabs = listOf("1. Form Web View", "2. AI Answers", "3. Review & Fill")

  Column(modifier = Modifier.fillMaxSize()) {
    // Top Tabs
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp
    ) {
      ScrollableTabRow(
        selectedTabIndex = selectedTab,
        edgePadding = 16.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
      ) {
        tabs.forEachIndexed { index, title ->
          Tab(
            selected = selectedTab == index,
            onClick = { viewModel.setWorkspaceTab(index) },
            text = {
              Text(
                text = title,
                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
              )
            },
            modifier = Modifier.testTag("workspace_tab_$index")
          )
        }
      }
    }

    if (isWebLoading && selectedTab == 0) {
      LinearProgressIndicator(
        progress = { webLoadingProgress / 100f },
        modifier = Modifier.fillMaxWidth().height(3.dp),
        color = MaterialTheme.colorScheme.primary
      )
    }

    // Tab 0: Live Form WebView
    when (selectedTab) {
      0 -> {
        Column(modifier = Modifier.fillMaxSize()) {
          // WebView Top Navigation & Action Bar
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = { if (webViewRef?.canGoBack() == true) webViewRef?.goBack() },
                  enabled = webViewRef?.canGoBack() == true,
                  modifier = Modifier.size(36.dp)
                ) {
                  Icon(Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(18.dp))
                }
                IconButton(
                  onClick = { if (webViewRef?.canGoForward() == true) webViewRef?.goForward() },
                  enabled = webViewRef?.canGoForward() == true,
                  modifier = Modifier.size(36.dp)
                ) {
                  Icon(Icons.Default.ArrowForward, contentDescription = "Forward", modifier = Modifier.size(18.dp))
                }
                IconButton(
                  onClick = { webViewRef?.reload() },
                  modifier = Modifier.size(36.dp)
                ) {
                  Icon(Icons.Default.Refresh, contentDescription = "Reload", modifier = Modifier.size(18.dp))
                }
              }

              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Scan questions button
                Button(
                  onClick = {
                    webViewRef?.evaluateJavascript(GoogleFormsBridge.EXTRACTION_SCRIPT, null)
                    Toast.makeText(context, "Scanning questions from form...", Toast.LENGTH_SHORT).show()
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                  modifier = Modifier.height(34.dp).testTag("scan_dom_btn")
                ) {
                  Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Scan", style = MaterialTheme.typography.labelSmall)
                }

                // Open externally in Chrome for SSO / Org auth
                IconButton(
                  onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formUrl))
                    context.startActivity(intent)
                  },
                  modifier = Modifier.size(36.dp).testTag("open_chrome_btn")
                ) {
                  Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in Chrome", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
              }
            }
          }

          // In-App WebView
          Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
              factory = { ctx ->
                WebView(ctx).apply {
                  layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                  )
                  settings.javaScriptEnabled = true
                  settings.domStorageEnabled = true
                  settings.loadWithOverviewMode = true
                  settings.useWideViewPort = true
                  settings.setSupportZoom(true)
                  settings.builtInZoomControls = true
                  settings.displayZoomControls = false

                  // JavaScript Interface Bridge
                  addJavascriptInterface(
                    GoogleFormsBridge(
                      onQuestionsExtracted = { title, extracted ->
                        post {
                          viewModel.onQuestionsExtractedFromWeb(title, extracted)
                          Toast.makeText(ctx, "Extracted ${extracted.size} questions!", Toast.LENGTH_SHORT).show()
                        }
                      },
                      onFillReport = { success, missed, msg ->
                        post {
                          viewModel.onFillReportReceived(success, missed, msg)
                          Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                        }
                      }
                    ),
                    GoogleFormsBridge.JS_INTERFACE_NAME
                  )

                  webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                      super.onPageFinished(view, url)
                      isWebLoading = false
                      // Inject DOM extraction script on page finish
                      view?.evaluateJavascript(GoogleFormsBridge.EXTRACTION_SCRIPT, null)
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                      return false // keep navigation in WebView
                    }
                  }

                  webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                      webLoadingProgress = newProgress
                      isWebLoading = newProgress < 100
                    }
                  }

                  loadUrl(formUrl)
                  webViewRef = this
                }
              },
              update = { view ->
                if (view.url != formUrl && formUrl.isNotBlank()) {
                  view.loadUrl(formUrl)
                }
                webViewRef = view
              },
              modifier = Modifier.fillMaxSize()
            )
          }
        }
      }

      // Tab 1: AI Answers
      1 -> {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
          Spacer(modifier = Modifier.height(12.dp))

          // Workspace Header Action Bar
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = formTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "${questions.size} questions detected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                  )
                }

                Button(
                  onClick = { viewModel.generateAllAnswers() },
                  enabled = !isGenerating && questions.isNotEmpty(),
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                  modifier = Modifier.testTag("generate_all_answers_btn")
                ) {
                  if (isGenerating) {
                    CircularProgressIndicator(
                      modifier = Modifier.size(18.dp),
                      color = MaterialTheme.colorScheme.onPrimary,
                      strokeWidth = 2.dp
                    )
                  } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate")
                  }
                }
              }

              if (generationError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                  Text(
                    text = generationError ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          if (questions.isEmpty()) {
            Box(
              modifier = Modifier.weight(1f).fillMaxWidth(),
              contentAlignment = Alignment.Center
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Text("No questions loaded yet", style = MaterialTheme.typography.titleMedium)
                Text("Open a form in the Form Web View tab or try the demo form.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(
                  onClick = { viewModel.loadSampleDemoForm() },
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                  Text("Load Demo Form Questions")
                }
              }
            }
          } else {
            LazyColumn(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              items(questions, key = { it.id }) { question ->
                AnswerCard(
                  question = question,
                  onUpdateAnswer = { viewModel.updateAnswer(question.id, it) },
                  onToggleChoice = { viewModel.toggleChoice(question.id, it) },
                  onRegenerate = { viewModel.regenerateSingleAnswer(question.id) },
                  onToggleLock = { viewModel.toggleLock(question.id) },
                  onToggleVerified = { viewModel.toggleVerified(question.id) }
                )
              }
              item { Spacer(modifier = Modifier.height(16.dp)) }
            }
          }
        }
      }

      // Tab 2: Review & Fill Screen
      2 -> {
        val answeredCount = questions.count { it.suggestedAnswer.isNotBlank() || it.selectedChoices.isNotEmpty() }
        val missingRequired = questions.filter { it.isRequired && it.suggestedAnswer.isBlank() && it.selectedChoices.isEmpty() }
        val uncertain = questions.filter { it.confidence.equals("NEEDS_REVIEW", ignoreCase = true) }

        LazyColumn(
          modifier = Modifier.fillMaxSize().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // Progress & Health Card
          item {
            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text("Form Review Summary", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))

                val pct = if (questions.isNotEmpty()) (answeredCount * 100 / questions.size) else 0
                Text("$answeredCount / ${questions.size} Questions Answered ($pct%)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                  progress = { if (questions.isNotEmpty()) answeredCount.toFloat() / questions.size else 0f },
                  modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                  color = if (pct == 100) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  if (missingRequired.isNotEmpty()) {
                    Surface(
                      color = Color(0xFFEF4444).copy(alpha = 0.15f),
                      shape = RoundedCornerShape(8.dp)
                    ) {
                      Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${missingRequired.size} Required Missing", style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444))
                      }
                    }
                  } else {
                    Surface(
                      color = Color(0xFF10B981).copy(alpha = 0.15f),
                      shape = RoundedCornerShape(8.dp)
                    ) {
                      Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("All Required Answered", style = MaterialTheme.typography.labelSmall, color = Color(0xFF10B981))
                      }
                    }
                  }

                  if (uncertain.isNotEmpty()) {
                    Surface(
                      color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                      shape = RoundedCornerShape(8.dp)
                    ) {
                      Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${uncertain.size} Needs Verification", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF59E0B))
                      }
                    }
                  }
                }
              }
            }
          }

          // Primary Execution Actions
          item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              // 1. Fill into Form
              Button(
                onClick = {
                  val script = GoogleFormsBridge.buildFillScript(questions)
                  viewModel.setWorkspaceTab(0) // Switch to webview
                  webViewRef?.evaluateJavascript(script, null)
                  Toast.makeText(context, "Executing auto-fill into Google Form fields...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("fill_into_form_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
              ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Auto-Fill Answers Into Form", fontWeight = FontWeight.Bold)
              }

              // 2. Copy All Answers
              OutlinedButton(
                onClick = {
                  val formatted = buildString {
                    appendLine("=== FormPilot AI Generated Answers ===")
                    appendLine("Form: $formTitle")
                    appendLine("URL: $formUrl")
                    appendLine("=====================================")
                    questions.forEach { q ->
                      appendLine()
                      appendLine("Q#${q.questionIndex + 1} [${q.questionType}]: ${q.questionText}")
                      appendLine("Answer: ${q.suggestedAnswer}")
                    }
                  }
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  val clip = ClipData.newPlainText("FormPilot Answers", formatted)
                  clipboard.setPrimaryClip(clip)
                  Toast.makeText(context, "All answers copied to clipboard!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("copy_all_answers_btn")
              ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy All Answers Formatted")
              }

              // 3. AI Consistency & Conflict Checker
              OutlinedButton(
                onClick = {
                  viewModel.runConsistencyCheck()
                  showConsistencyDialog = true
                },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("consistency_check_btn")
              ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Run Consistency & Conflict Check")
              }
            }
          }

          // Fill Report notice if available
          if (fillReport != null) {
            item {
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Text("Last Fill Result", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "${fillReport!!.filledCount} of ${fillReport!!.totalQuestions} questions filled.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                  )
                  fillReport!!.details.forEach { d ->
                    Text(text = "• $d", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }
              }
            }
          }

          // Safety & Submission Guideline Box
          item {
            Surface(
              color = Color(0xFF1E293B),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Text(
                  text = "Manual Submission Notice",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = Color(0xFF38BDF8)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "FormPilot AI will assist you by inserting and formatting answers. For your security and compliance with Google's policies, you must review every field and click the form's native 'Submit' button yourself.",
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                  color = Color(0xFF94A3B8)
                )
              }
            }
          }
        }
      }
    }
  }

  // Consistency & Conflict Report Dialog
  if (showConsistencyDialog) {
    AlertDialog(
      onDismissRequest = { showConsistencyDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
          Text("AI Consistency Analysis")
        }
      },
      text = {
        if (isAnalyzingConsistency) {
          Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
          }
        } else {
          Text(
            text = consistencyReport ?: "Analyzing form responses...",
            style = MaterialTheme.typography.bodyMedium
          )
        }
      },
      confirmButton = {
        TextButton(onClick = { showConsistencyDialog = false }) {
          Text("Done")
        }
      }
    )
  }
}
