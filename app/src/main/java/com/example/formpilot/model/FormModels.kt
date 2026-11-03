package com.example.formpilot.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class QuestionType(val displayName: String, val badgeColorHex: Long) {
  SHORT_ANSWER("Short Answer", 0xFF38BDF8),
  PARAGRAPH("Paragraph", 0xFF818CF8),
  MULTIPLE_CHOICE("Multiple Choice", 0xFF34D399),
  CHECKBOXES("Checkboxes", 0xFFFBBF24),
  DROPDOWN("Dropdown", 0xFFA78BFA),
  LINEAR_SCALE("Linear Scale", 0xFFF472B6),
  DATE("Date", 0xFF60A5FA),
  TIME("Time", 0xFF4ADE80),
  TRUE_FALSE("True / False", 0xFF2DD4BF),
  OTHER("General Field", 0xFF94A3B8);

  companion object {
    fun fromRaw(typeStr: String): QuestionType {
      val lower = typeStr.lowercase()
      return when {
        "short" in lower -> SHORT_ANSWER
        "paragraph" in lower || "long" in lower -> PARAGRAPH
        "choice" in lower || "radio" in lower -> MULTIPLE_CHOICE
        "check" in lower -> CHECKBOXES
        "drop" in lower || "select" in lower -> DROPDOWN
        "scale" in lower || "rate" in lower || "rating" in lower -> LINEAR_SCALE
        "date" in lower -> DATE
        "time" in lower -> TIME
        "true" in lower || "false" in lower || "boolean" in lower -> TRUE_FALSE
        else -> SHORT_ANSWER
      }
    }
  }
}

enum class AnswerMode(val title: String, val description: String, val iconName: String) {
  QUICK("Quick Answer", "Concise, straight to the point responses", "bolt"),
  DETAILED("Detailed Answer", "In-depth rationale with full explanations", "notes"),
  NATURAL("Natural Writing", "Conversational, genuine human tone", "person"),
  FORMAL("Formal Academic", "Professional, high-level vocabulary", "school"),
  SIMPLE_ENGLISH("Simple English", "Accessible, straightforward wording", "chat"),
  FILIPINO("Filipino / Taglish", "Natural conversational Tagalog or Taglish", "translate")
}

enum class ConfidenceLevel(val label: String, val colorHex: Long) {
  HIGH("High Confidence", 0xFF10B981),
  MEDIUM("Medium Confidence", 0xFFF59E0B),
  LOW("Low Confidence", 0xFFF97316),
  NEEDS_REVIEW("Needs Clarification", 0xFFEF4444)
}

@IgnoreExtraProperties
data class FormQuestion(
  var id: String = "",
  var formId: String = "",
  var questionIndex: Int = 0,
  var pageIndex: Int = 1,
  var questionText: String = "",
  var questionType: String = QuestionType.SHORT_ANSWER.name,
  var isRequired: Boolean = false,
  var options: List<String> = emptyList(),
  var suggestedAnswer: String = "",
  var selectedChoices: List<String> = emptyList(),
  var isLocked: Boolean = false,
  var isVerified: Boolean = false,
  var confidence: String = ConfidenceLevel.HIGH.name,
  var reasoning: String = "",
  var domSelector: String = "",
  var entryName: String = "",
  @ServerTimestamp var updatedAt: Timestamp? = null
) {
  fun getType(): QuestionType = try {
    QuestionType.valueOf(questionType)
  } catch (_: Exception) {
    QuestionType.SHORT_ANSWER
  }

  fun getConfidenceLevel(): ConfidenceLevel = try {
    ConfidenceLevel.valueOf(confidence)
  } catch (_: Exception) {
    ConfidenceLevel.MEDIUM
  }
}

data class FormPageState(
  val currentPage: Int = 1,
  val totalPages: Int = 1,
  val pageIndicatorText: String = "Page 1 of 1",
  val hasNextPage: Boolean = false,
  val hasPrevPage: Boolean = false,
  val isSubmitPage: Boolean = false,
  val sectionTitle: String = ""
)

@IgnoreExtraProperties
data class FormSession(
  @DocumentId var id: String = "",
  var userId: String = "",
  var formUrl: String = "",
  var formTitle: String = "Untitled Form",
  var detectedQuestionsCount: Int = 0,
  var completedQuestionsCount: Int = 0,
  var status: String = "draft",
  @ServerTimestamp var createdAt: Timestamp? = null,
  @ServerTimestamp var updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId var userId: String = "",
  var displayName: String = "",
  var email: String = "",
  var photoUrl: String? = null,
  var schoolOrOrg: String = "",
  var department: String = "",
  var preferredTone: String = AnswerMode.NATURAL.name,
  var preferredLanguage: String = "English",
  var notes: String = "",
  var memoryEnabled: Boolean = true,
  @ServerTimestamp var createdAt: Timestamp? = null,
  @ServerTimestamp var updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AnswerTemplate(
  @DocumentId var id: String = "",
  var userId: String = "",
  var title: String = "",
  var category: String = "General",
  var content: String = "",
  var tags: List<String> = emptyList(),
  @ServerTimestamp var createdAt: Timestamp? = null,
  @ServerTimestamp var updatedAt: Timestamp? = null
)

data class ReferenceDocument(
  val id: String,
  val name: String,
  val content: String,
  val sizeKb: Int,
  val isSelected: Boolean = true
)

data class FormFillReport(
  val totalQuestions: Int,
  val filledCount: Int,
  val unverifiedCount: Int,
  val details: List<String>
)
