package com.example.formpilot.service

import android.graphics.Bitmap
import android.util.Log
import com.example.formpilot.model.AnswerMode
import com.example.formpilot.model.ConfidenceLevel
import com.example.formpilot.model.FormQuestion
import com.example.formpilot.model.QuestionType
import com.example.formpilot.model.ReferenceDocument
import com.example.formpilot.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "GeminiFormService"

class GeminiFormService {

  // Standard model for JSON structured question answering
  private val jsonModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      generationConfig = generationConfig {
        responseMimeType = "application/json"
      }
    )
  }

  // Vision / Multimodal model for screenshot question recognition and chat
  private val textModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash"
    )
  }

  // Generate smart answers for a list of questions
  suspend fun generateAnswers(
    questions: List<FormQuestion>,
    mode: AnswerMode,
    profile: UserProfile?,
    referenceDocs: List<ReferenceDocument>,
    customInstructions: String,
    answerLength: String = "standard" // concise, standard, detailed
  ): List<FormQuestion> {
    if (questions.isEmpty()) return emptyList()

    // Filter unlocked questions for generation
    val questionsToAnswer = questions.filter { !it.isLocked }
    if (questionsToAnswer.isEmpty()) return questions

    return withTimeout(60_000L) {
      try {
        val prompt = buildString {
          appendLine("You are FormPilot AI, an expert Google Forms answering assistant.")
          appendLine("Your task is to generate intelligent, context-aware, and accurate answers for the following Google Forms questions.")
          appendLine()
          appendLine("SELECTED ANSWERING MODE: ${mode.title} - ${mode.description}")
          appendLine("ANSWER LENGTH TARGET: $answerLength")
          appendLine()

          if (profile != null && profile.memoryEnabled) {
            appendLine("USER PERSONAL PROFILE & CONTEXT (Use strictly when relevant to personal questions):")
            if (profile.displayName.isNotBlank()) appendLine("- Name: ${profile.displayName}")
            if (profile.schoolOrOrg.isNotBlank()) appendLine("- School / Organization: ${profile.schoolOrOrg}")
            if (profile.department.isNotBlank()) appendLine("- Course / Department: ${profile.department}")
            if (profile.notes.isNotBlank()) appendLine("- Personal Notes & Background: ${profile.notes}")
            appendLine()
          }

          val activeDocs = referenceDocs.filter { it.isSelected }
          if (activeDocs.isNotEmpty()) {
            appendLine("SUPPLIED REFERENCE DOCUMENTS / SOURCE MATERIAL:")
            for (doc in activeDocs) {
              appendLine("--- Document: ${doc.name} ---")
              appendLine(doc.content.take(4000))
              appendLine("---------------------------")
            }
            appendLine("CRITICAL: Prioritize answers based on the supplied reference material whenever applicable.")
            appendLine()
          }

          if (customInstructions.isNotBlank()) {
            appendLine("USER CUSTOM INSTRUCTIONS:")
            appendLine(customInstructions)
            appendLine()
          }

          appendLine("RULES FOR ANSWERING:")
          appendLine("1. For MULTIPLE_CHOICE: Your 'suggestedAnswer' MUST be one of the provided options verbatim.")
          appendLine("2. For CHECKBOXES: Provide 'selectedChoices' as an array of option strings matching the provided options, and 'suggestedAnswer' as comma-separated choices.")
          appendLine("3. For SHORT_ANSWER: Keep answer clear, concise, and direct.")
          appendLine("4. For PARAGRAPH: Write a well-structured, natural human response appropriate to the selected mode.")
          appendLine("5. For LINEAR_SCALE / RATING: Choose an appropriate rating number from the scale.")
          appendLine("6. For DATE: Format as YYYY-MM-DD or standard readable date.")
          appendLine("7. If a question asks for personal information not in the profile or documents, set confidence to 'needs_clarification' and suggest a placeholder response.")
          appendLine("8. In Filipino mode: Generate answers in natural Filipino / Taglish.")
          appendLine()
          appendLine("RETURN A STRICT JSON ARRAY of objects with the following keys:")
          appendLine("- 'questionIndex': integer (matches question index)")
          appendLine("- 'suggestedAnswer': string (the suggested answer text)")
          appendLine("- 'selectedChoices': array of strings (for checkbox/choice questions)")
          appendLine("- 'confidence': string ('high', 'medium', 'low', or 'needs_clarification')")
          appendLine("- 'reasoning': string (brief 1-sentence rationale for the answer)")
          appendLine()
          appendLine("QUESTIONS TO ANSWER:")
          for (q in questionsToAnswer) {
            appendLine("Question #${q.questionIndex}: [${q.questionType}] ${q.questionText} (Required: ${q.isRequired})")
            if (q.options.isNotEmpty()) {
              appendLine("Options: ${q.options.joinToString(" | ")}")
            }
          }
        }

        val response = jsonModel.generateContent(prompt)
        val responseText = response.text ?: throw IllegalStateException("Model returned empty text.")

        val jsonArray = JSONArray(responseText.trim())
        val resultMap = mutableMapOf<Int, JSONObject>()
        for (i in 0 until jsonArray.length()) {
          val obj = jsonArray.getJSONObject(i)
          resultMap[obj.optInt("questionIndex", -1)] = obj
        }

        questions.map { original ->
          if (original.isLocked) {
            original
          } else {
            val genObj = resultMap[original.questionIndex]
            if (genObj != null) {
              val ans = genObj.optString("suggestedAnswer", "")
              val conf = genObj.optString("confidence", "high").uppercase()
              val reason = genObj.optString("reasoning", "")
              val choicesArr = genObj.optJSONArray("selectedChoices") ?: JSONArray()
              val choices = (0 until choicesArr.length()).map { choicesArr.getString(it) }

              original.copy(
                suggestedAnswer = ans,
                selectedChoices = if (choices.isNotEmpty()) choices else listOf(ans).filter { it.isNotBlank() },
                confidence = try {
                  ConfidenceLevel.valueOf(conf).name
                } catch (_: Exception) {
                  ConfidenceLevel.HIGH.name
                },
                reasoning = reason
              )
            } else {
              original
            }
          }
        }
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Generation timed out", e)
        throw e
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Failed to generate answers", e)
        throw e
      }
    }
  }

  // Multimodal Screenshot Recognition: Extract questions from a form screenshot
  suspend fun extractQuestionsFromScreenshot(bitmap: Bitmap): Pair<String, List<FormQuestion>> {
    return withTimeout(60_000L) {
      try {
        val prompt = """
          Analyze this Google Form screenshot.
          Extract:
          1. The title of the form if visible.
          2. All visible questions with their:
             - questionIndex (0-based)
             - questionText
             - questionType (SHORT_ANSWER, PARAGRAPH, MULTIPLE_CHOICE, CHECKBOXES, LINEAR_SCALE, DATE, TIME, OTHER)
             - isRequired (boolean, check for asterisk *)
             - options (array of strings if multiple choice or checkboxes)

          Return a STRICT JSON object in this format:
          {
            "title": "Title of the Form",
            "questions": [
              {
                "questionIndex": 0,
                "questionText": "...",
                "questionType": "SHORT_ANSWER",
                "isRequired": false,
                "options": []
              }
            ]
          }
        """.trimIndent()

        val response = jsonModel.generateContent(
          content {
            image(bitmap)
            text(prompt)
          }
        )

        val jsonText = response.text ?: throw IllegalStateException("Model returned empty response.")
        val root = JSONObject(jsonText.trim())
        val title = root.optString("title", "Scanned Google Form")
        val questionsArr = root.optJSONArray("questions") ?: JSONArray()
        val resultList = mutableListOf<FormQuestion>()

        for (i in 0 until questionsArr.length()) {
          val qObj = questionsArr.getJSONObject(i)
          val typeStr = qObj.optString("questionType", "SHORT_ANSWER")
          val optionsArr = qObj.optJSONArray("options") ?: JSONArray()
          val options = (0 until optionsArr.length()).map { optionsArr.getString(it) }

          resultList.add(
            FormQuestion(
              id = "scan_$i",
              questionIndex = qObj.optInt("questionIndex", i),
              questionText = qObj.optString("questionText", "Question ${i + 1}"),
              questionType = QuestionType.fromRaw(typeStr).name,
              isRequired = qObj.optBoolean("isRequired", false),
              options = options,
              confidence = ConfidenceLevel.HIGH.name
            )
          )
        }

        Pair(title, resultList)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Screenshot question extraction failed", e)
        throw e
      }
    }
  }

  // Parse raw pasted text into structured questions
  suspend fun parsePastedQuestions(text: String): Pair<String, List<FormQuestion>> {
    return withTimeout(60_000L) {
      try {
        val prompt = """
          Parse the following copied Google Form text or raw questions into structured question objects.
          Extract:
          - Form title (infer from text or header)
          - Questions list with questionText, questionType, isRequired, options

          TEXT CONTENT:
          $text

          Return a STRICT JSON object:
          {
            "title": "Form Title",
            "questions": [
              {
                "questionIndex": 0,
                "questionText": "What is your name?",
                "questionType": "SHORT_ANSWER",
                "isRequired": true,
                "options": []
              }
            ]
          }
        """.trimIndent()

        val response = jsonModel.generateContent(prompt)
        val jsonText = response.text ?: throw IllegalStateException("Empty response from AI")
        val root = JSONObject(jsonText.trim())
        val title = root.optString("title", "Imported Form")
        val qArray = root.optJSONArray("questions") ?: JSONArray()
        val list = mutableListOf<FormQuestion>()

        for (i in 0 until qArray.length()) {
          val obj = qArray.getJSONObject(i)
          val optionsArr = obj.optJSONArray("options") ?: JSONArray()
          val options = (0 until optionsArr.length()).map { optionsArr.getString(it) }
          list.add(
            FormQuestion(
              id = "paste_$i",
              questionIndex = obj.optInt("questionIndex", i),
              questionText = obj.optString("questionText", "Question ${i + 1}"),
              questionType = QuestionType.fromRaw(obj.optString("questionType", "SHORT_ANSWER")).name,
              isRequired = obj.optBoolean("isRequired", false),
              options = options
            )
          )
        }
        Pair(title, list)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Pasted text parsing failed", e)
        throw e
      }
    }
  }

  // Smart Form Summary & Consistency Analysis
  suspend fun analyzeFormSummaryAndConsistency(questions: List<FormQuestion>): String {
    if (questions.isEmpty()) return "No questions to analyze."
    return withTimeout(60_000L) {
      try {
        val qSummary = questions.joinToString("\n") { q ->
          "Q#${q.questionIndex}: [${q.questionType}] ${q.questionText} | Answer: '${q.suggestedAnswer}'"
        }
        val prompt = """
          Analyze this completed Google Form submission for:
          1. Form Summary: Brief 2-sentence summary of what this form is for.
          2. Consistency Check: Identify any conflicting, contradictory, or duplicate answers.
          3. Missing or Ambiguous Info: Any question that might need clarification before manual submit.

          FORM DATA:
          $qSummary

          Provide concise bullet points with helpful emoji markers.
        """.trimIndent()

        val response = textModel.generateContent(prompt)
        response.text ?: "Analysis complete. All answers appear consistent."
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        "Consistency check unavailable: ${e.localizedMessage}"
      }
    }
  }
}
