package com.example.formpilot.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.formpilot.model.ConfidenceLevel
import com.example.formpilot.model.FormQuestion
import com.example.formpilot.model.QuestionType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnswerCard(
  question: FormQuestion,
  onUpdateAnswer: (String) -> Unit,
  onToggleChoice: (String) -> Unit,
  onRegenerate: () -> Unit,
  onToggleLock: () -> Unit,
  onToggleVerified: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var isEditing by remember { mutableStateOf(false) }
  var editedText by remember(question.suggestedAnswer) { mutableStateOf(question.suggestedAnswer) }
  var showReasoning by remember { mutableStateOf(false) }

  val qType = question.getType()
  val confLevel = question.getConfidenceLevel()
  val typeBadgeColor = Color(qType.badgeColorHex)
  val confColor = Color(confLevel.colorHex)

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("answer_card_${question.questionIndex}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = CardDefaults.outlinedCardBorder().copy(
      width = if (question.isVerified) 1.5.dp else 1.dp,
      brush = if (question.isVerified) {
        androidx.compose.ui.graphics.SolidColor(Color(0xFF10B981))
      } else {
        androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline)
      }
    )
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Header: Question index, type badge, required asterisk, lock status
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Box(
            modifier = Modifier
              .size(26.dp)
              .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "${question.questionIndex + 1}",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }

          Surface(
            color = typeBadgeColor.copy(alpha = 0.15f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = qType.displayName,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
              color = typeBadgeColor,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }

          if (question.isRequired) {
            Surface(
              color = Color(0xFFEF4444).copy(alpha = 0.15f),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = "Required",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFFEF4444),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
              )
            }
          }
        }

        // Lock & Verified Badges
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onToggleLock,
            modifier = Modifier.size(32.dp).testTag("lock_btn_${question.questionIndex}")
          ) {
            Icon(
              imageVector = if (question.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
              contentDescription = if (question.isLocked) "Locked" else "Unlocked",
              tint = if (question.isLocked) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
          }

          IconButton(
            onClick = onToggleVerified,
            modifier = Modifier.size(32.dp).testTag("verify_btn_${question.questionIndex}")
          ) {
            Icon(
              imageVector = if (question.isVerified) Icons.Default.Verified else Icons.Outlined.Verified,
              contentDescription = if (question.isVerified) "Verified" else "Not verified",
              tint = if (question.isVerified) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Question Prompt Text
      Text(
        text = question.questionText,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Content based on question type
      when (qType) {
        QuestionType.MULTIPLE_CHOICE, QuestionType.LINEAR_SCALE, QuestionType.TRUE_FALSE -> {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            question.options.forEach { option ->
              val isSelected = question.selectedChoices.contains(option) || question.suggestedAnswer.equals(option, ignoreCase = true)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant)
                  .clickable { onToggleChoice(option) }
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                RadioButton(
                  selected = isSelected,
                  onClick = { onToggleChoice(option) },
                  colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = option,
                  style = MaterialTheme.typography.bodyMedium,
                  color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }

        QuestionType.CHECKBOXES -> {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            question.options.forEach { option ->
              val isChecked = question.selectedChoices.contains(option)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant)
                  .clickable { onToggleChoice(option) }
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Checkbox(
                  checked = isChecked,
                  onCheckedChange = { onToggleChoice(option) },
                  colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = option,
                  style = MaterialTheme.typography.bodyMedium,
                  color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }

        else -> {
          // Short Answer, Paragraph, Date, Time, etc.
          if (isEditing) {
            Column {
              OutlinedTextField(
                value = editedText,
                onValueChange = { editedText = it },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("edit_input_${question.questionIndex}"),
                label = { Text("Suggested Answer") },
                minLines = if (qType == QuestionType.PARAGRAPH) 3 else 1
              )
              Spacer(modifier = Modifier.height(6.dp))
              Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { isEditing = false }) {
                  Text("Cancel", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(
                  onClick = {
                    onUpdateAnswer(editedText)
                    isEditing = false
                  }
                ) {
                  Icon(Icons.Default.CheckCircle, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                }
              }
            }
          } else {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
              ) {
                Text(
                  text = question.suggestedAnswer.ifBlank { "No answer generated yet." },
                  style = MaterialTheme.typography.bodyMedium,
                  color = if (question.suggestedAnswer.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.weight(1f)
                )
                IconButton(
                  onClick = {
                    editedText = question.suggestedAnswer
                    isEditing = true
                  },
                  modifier = Modifier.size(28.dp).testTag("edit_btn_${question.questionIndex}")
                ) {
                  Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit answer",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Footer: Confidence Pill, Rationale toggle, Copy button, Regenerate button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Confidence Pill
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(confColor.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Box(modifier = Modifier.size(6.dp).background(confColor, shape = CircleShape))
          Text(
            text = confLevel.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
            color = confColor
          )
        }

        // Actions: Rationale, Copy, Regenerate
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
          if (question.reasoning.isNotBlank()) {
            IconButton(
              onClick = { showReasoning = !showReasoning },
              modifier = Modifier.size(32.dp).testTag("rationale_btn_${question.questionIndex}")
            ) {
              Icon(
                imageVector = if (showReasoning) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = "Reasoning",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          IconButton(
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              val clip = ClipData.newPlainText("Answer", question.suggestedAnswer)
              clipboard.setPrimaryClip(clip)
              Toast.makeText(context, "Answer copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(32.dp).testTag("copy_btn_${question.questionIndex}")
          ) {
            Icon(
              imageVector = Icons.Default.ContentCopy,
              contentDescription = "Copy answer",
              modifier = Modifier.size(16.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          IconButton(
            onClick = onRegenerate,
            enabled = !question.isLocked,
            modifier = Modifier.size(32.dp).testTag("regenerate_btn_${question.questionIndex}")
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Regenerate answer",
              modifier = Modifier.size(16.dp),
              tint = if (!question.isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
          }
        }
      }

      // Rationale collapsible accordion
      AnimatedVisibility(visible = showReasoning && question.reasoning.isNotBlank()) {
        Column(modifier = Modifier.padding(top = 8.dp)) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "AI Rationale: ${question.reasoning}",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(10.dp)
            )
          }
        }
      }
    }
  }
}
