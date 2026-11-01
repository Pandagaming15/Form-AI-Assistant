package com.example.formpilot

import com.example.formpilot.model.ConfidenceLevel
import com.example.formpilot.model.FormQuestion
import com.example.formpilot.model.QuestionType
import com.example.formpilot.service.GoogleFormsBridge
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FormPilotUnitTest {

  @Test
  fun testGoogleFormsUrlValidation() {
    assertTrue(GoogleFormsBridge.isValidGoogleFormUrl("https://docs.google.com/forms/d/e/1FAIpQLSc_example/viewform"))
    assertTrue(GoogleFormsBridge.isValidGoogleFormUrl("https://forms.gle/xyz123abc"))
    assertTrue(GoogleFormsBridge.isValidGoogleFormUrl("http://docs.google.com/forms/d/12345/edit"))

    assertFalse(GoogleFormsBridge.isValidGoogleFormUrl("https://google.com"))
    assertFalse(GoogleFormsBridge.isValidGoogleFormUrl("https://example.com/form"))
    assertFalse(GoogleFormsBridge.isValidGoogleFormUrl("invalid_url_string"))
  }

  @Test
  fun testUrlNormalization() {
    val raw = "docs.google.com/forms/d/e/test"
    val normalized = GoogleFormsBridge.normalizeFormUrl(raw)
    assertEquals("https://docs.google.com/forms/d/e/test", normalized)
  }

  @Test
  fun testQuestionTypeDetection() {
    assertEquals(QuestionType.SHORT_ANSWER, QuestionType.fromRaw("short answer"))
    assertEquals(QuestionType.PARAGRAPH, QuestionType.fromRaw("paragraph"))
    assertEquals(QuestionType.MULTIPLE_CHOICE, QuestionType.fromRaw("multiple choice"))
    assertEquals(QuestionType.CHECKBOXES, QuestionType.fromRaw("checkboxes"))
    assertEquals(QuestionType.DROPDOWN, QuestionType.fromRaw("dropdown"))
    assertEquals(QuestionType.LINEAR_SCALE, QuestionType.fromRaw("linear scale rating"))
    assertEquals(QuestionType.DATE, QuestionType.fromRaw("date"))
    assertEquals(QuestionType.TIME, QuestionType.fromRaw("time"))
  }

  @Test
  fun testMultipleChoiceAnswerMapping() {
    val q = FormQuestion(
      id = "q1",
      questionIndex = 0,
      questionText = "Preferred Platform",
      questionType = QuestionType.MULTIPLE_CHOICE.name,
      isRequired = true,
      options = listOf("Android", "iOS", "Web"),
      suggestedAnswer = "Android",
      selectedChoices = listOf("Android"),
      confidence = ConfidenceLevel.HIGH.name
    )

    assertEquals("Android", q.suggestedAnswer)
    assertTrue(q.options.contains(q.suggestedAnswer))
    assertEquals(1, q.selectedChoices.size)
    assertEquals(QuestionType.MULTIPLE_CHOICE, q.getType())
  }

  @Test
  fun testCheckboxAnswerMapping() {
    val q = FormQuestion(
      id = "q2",
      questionIndex = 1,
      questionText = "Select skills",
      questionType = QuestionType.CHECKBOXES.name,
      isRequired = false,
      options = listOf("Kotlin", "Java", "Python"),
      suggestedAnswer = "Kotlin, Python",
      selectedChoices = listOf("Kotlin", "Python")
    )

    assertEquals(2, q.selectedChoices.size)
    assertTrue(q.selectedChoices.contains("Kotlin"))
    assertTrue(q.selectedChoices.contains("Python"))
    assertFalse(q.selectedChoices.contains("Java"))
  }

  @Test
  fun testFillScriptGeneration() {
    val questions = listOf(
      FormQuestion(
        id = "q1",
        questionIndex = 0,
        questionText = "Your Name",
        questionType = QuestionType.SHORT_ANSWER.name,
        suggestedAnswer = "Kyle Laroza"
      ),
      FormQuestion(
        id = "q2",
        questionIndex = 1,
        questionText = "Favorite Framework",
        questionType = QuestionType.MULTIPLE_CHOICE.name,
        suggestedAnswer = "Jetpack Compose",
        selectedChoices = listOf("Jetpack Compose")
      )
    )

    val script = GoogleFormsBridge.buildFillScript(questions)
    assertNotNull(script)
    assertTrue(script.contains("questionsToFill"))
    assertTrue(script.contains("Kyle Laroza"))
    assertTrue(script.contains("Jetpack Compose"))
  }

  @Test
  fun testMissingRequiredQuestionDetection() {
    val questions = listOf(
      FormQuestion(
        id = "q1",
        questionIndex = 0,
        questionText = "Required Question 1",
        isRequired = true,
        suggestedAnswer = "Answered"
      ),
      FormQuestion(
        id = "q2",
        questionIndex = 1,
        questionText = "Required Question 2",
        isRequired = true,
        suggestedAnswer = ""
      ),
      FormQuestion(
        id = "q3",
        questionIndex = 2,
        questionText = "Optional Question",
        isRequired = false,
        suggestedAnswer = ""
      )
    )

    val missing = questions.filter { it.isRequired && it.suggestedAnswer.isBlank() && it.selectedChoices.isEmpty() }
    assertEquals(1, missing.size)
    assertEquals("q2", missing[0].id)
  }

  @Test
  fun testJsonParsingSimulation() {
    val sampleAiJson = """
      [
        {
          "questionIndex": 0,
          "suggestedAnswer": "Computer Science",
          "selectedChoices": ["Computer Science"],
          "confidence": "high",
          "reasoning": "Matched from profile"
        }
      ]
    """.trimIndent()

    val array = JSONArray(sampleAiJson)
    assertEquals(1, array.length())
    val item = array.getJSONObject(0)
    assertEquals(0, item.getInt("questionIndex"))
    assertEquals("Computer Science", item.getString("suggestedAnswer"))
    assertEquals("high", item.getString("confidence"))
  }
}
