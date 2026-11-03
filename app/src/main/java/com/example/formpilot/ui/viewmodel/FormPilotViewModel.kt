package com.example.formpilot.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.formpilot.auth.AuthManager
import com.example.formpilot.data.local.AppDatabase
import com.example.formpilot.data.local.DraftEntity
import com.example.formpilot.data.remote.FirestoreRepository
import com.example.formpilot.model.AnswerMode
import com.example.formpilot.model.AnswerTemplate
import com.example.formpilot.model.ConfidenceLevel
import com.example.formpilot.model.FormFillReport
import com.example.formpilot.model.FormQuestion
import com.example.formpilot.model.FormSession
import com.example.formpilot.model.QuestionType
import com.example.formpilot.model.ReferenceDocument
import com.example.formpilot.model.UserProfile
import com.example.formpilot.service.GeminiFormService
import com.example.formpilot.service.GoogleFormsBridge
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private const val TAG = "FormPilotVM"

class FormPilotViewModel(application: Application) : AndroidViewModel(application) {

  val authManager = AuthManager(application.applicationContext)
  val firestoreRepo = FirestoreRepository(application.applicationContext)
  val geminiService = GeminiFormService()
  val localDb = AppDatabase.getDatabase(application.applicationContext)

  // Auth state
  val currentUser: StateFlow<FirebaseUser?> = authManager.currentUser
  val isAuthLoading: StateFlow<Boolean> = authManager.isLoading
  val authError: StateFlow<String?> = authManager.errorMessage

  // Form State
  private val _formUrl = MutableStateFlow("https://docs.google.com/forms/d/e/1FAIpQLSc-sample/viewform")
  val formUrl: StateFlow<String> = _formUrl.asStateFlow()

  private val _formTitle = MutableStateFlow("College Student Feedback Survey 2026")
  val formTitle: StateFlow<String> = _formTitle.asStateFlow()

  private val _formStatus = MutableStateFlow("draft")
  val formStatus: StateFlow<String> = _formStatus.asStateFlow()

  private val _currentSessionId = MutableStateFlow(UUID.randomUUID().toString())
  val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

  private val _questions = MutableStateFlow<List<FormQuestion>>(emptyList())
  val questions: StateFlow<List<FormQuestion>> = _questions.asStateFlow()

  // Multi-Page Form State & Navigation
  private val _pageState = MutableStateFlow(
    com.example.formpilot.model.FormPageState(
      currentPage = 1,
      totalPages = 2,
      pageIndicatorText = "Page 1 of 2",
      hasNextPage = true,
      hasPrevPage = false,
      isSubmitPage = false,
      sectionTitle = "Personal & Background"
    )
  )
  val pageState: StateFlow<com.example.formpilot.model.FormPageState> = _pageState.asStateFlow()

  // Cache questions across visited pages
  private val _pageQuestionsMap = mutableMapOf<Int, List<FormQuestion>>()

  // Auto AI Generation on page navigation: AI automatically analyzes and generates answers when user navigates
  private val _autoAiOnPageChange = MutableStateFlow(true)
  val autoAiOnPageChange: StateFlow<Boolean> = _autoAiOnPageChange.asStateFlow()

  private val _isNavigatingPage = MutableStateFlow(false)
  val isNavigatingPage: StateFlow<Boolean> = _isNavigatingPage.asStateFlow()

  private val _pageNavigationMessage = MutableStateFlow<String?>(null)
  val pageNavigationMessage: StateFlow<String?> = _pageNavigationMessage.asStateFlow()

  // AI Configuration
  private val _selectedMode = MutableStateFlow(AnswerMode.NATURAL)
  val selectedMode: StateFlow<AnswerMode> = _selectedMode.asStateFlow()

  private val _customInstructions = MutableStateFlow("")
  val customInstructions: StateFlow<String> = _customInstructions.asStateFlow()

  private val _answerLength = MutableStateFlow("standard") // concise, standard, detailed
  val answerLength: StateFlow<String> = _answerLength.asStateFlow()

  // Generation & Status
  private val _isGenerating = MutableStateFlow(false)
  val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

  private val _generationError = MutableStateFlow<String?>(null)
  val generationError: StateFlow<String?> = _generationError.asStateFlow()

  private val _fillReport = MutableStateFlow<FormFillReport?>(null)
  val fillReport: StateFlow<FormFillReport?> = _fillReport.asStateFlow()

  private val _consistencyReport = MutableStateFlow<String?>(null)
  val consistencyReport: StateFlow<String?> = _consistencyReport.asStateFlow()

  private val _isAnalyzingConsistency = MutableStateFlow(false)
  val isAnalyzingConsistency: StateFlow<Boolean> = _isAnalyzingConsistency.asStateFlow()

  // Navigation tab for workspace (0: Live Form WebView, 1: AI Answers, 2: Review & Fill)
  private val _workspaceTab = MutableStateFlow(1)
  val workspaceTab: StateFlow<Int> = _workspaceTab.asStateFlow()

  // User Profile
  private val _userProfile = MutableStateFlow<UserProfile?>(null)
  val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

  // Answer Library & Templates
  private val _templates = MutableStateFlow<List<AnswerTemplate>>(emptyList())
  val templates: StateFlow<List<AnswerTemplate>> = _templates.asStateFlow()

  // Cloud Drafts
  private val _cloudDrafts = MutableStateFlow<List<FormSession>>(emptyList())
  val cloudDrafts: StateFlow<List<FormSession>> = _cloudDrafts.asStateFlow()

  // Local Drafts Flow from Room
  val localDrafts: StateFlow<List<DraftEntity>> = localDb.draftDao().getAllDrafts()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  // Reference materials for AI
  private val _referenceDocs = MutableStateFlow<List<ReferenceDocument>>(
    listOf(
      ReferenceDocument(
        id = "doc_1",
        name = "Student_Academic_Bio.txt",
        content = "Name: Kyle Airon Laroza. University: State University of Technology. Major: Computer Engineering, 3rd Year. Skills: Kotlin, Python, Mobile Systems, Embedded AI. GPA: 3.85. Available for internships starting June 2026.",
        sizeKb = 2,
        isSelected = true
      )
    )
  )
  val referenceDocs: StateFlow<List<ReferenceDocument>> = _referenceDocs.asStateFlow()

  init {
    // Attempt silent auto-sign in
    authManager.attemptAutoSignIn(viewModelScope)

    // Load initial sample demo questions so user can immediately experience the AI engine
    loadSampleDemoForm()

    // Observe user profile and cloud drafts when signed in
    viewModelScope.launch {
      currentUser.collect { user ->
        if (user != null) {
          launch {
            firestoreRepo.observeUserProfile().collect { profile ->
              _userProfile.value = profile ?: UserProfile(
                userId = user.uid,
                displayName = user.displayName ?: "Kyle Airon Laroza",
                email = user.email ?: "kyle.example@gmail.com",
                schoolOrOrg = "State University",
                department = "Computer Engineering",
                preferredTone = AnswerMode.NATURAL.name,
                preferredLanguage = "English",
                notes = "3rd year student, loves mobile software design and AI systems.",
                memoryEnabled = true
              )
            }
          }
          launch {
            firestoreRepo.observeFormSessions().collect { sessions ->
              _cloudDrafts.value = sessions
            }
          }
          launch {
            firestoreRepo.observeAnswerTemplates().collect { temps ->
              if (temps.isEmpty()) {
                // Initialize with some default templates
                initDefaultTemplates()
              } else {
                _templates.value = temps
              }
            }
          }
        } else {
          _cloudDrafts.value = emptyList()
        }
      }
    }
  }

  private fun initDefaultTemplates() {
    viewModelScope.launch {
      val defaultTemplates = listOf(
        AnswerTemplate(
          id = "temp_1",
          userId = currentUser.value?.uid ?: "",
          title = "Self Introduction",
          category = "Personal",
          content = "I am a dedicated Computer Engineering student passionate about building practical, user-friendly mobile and web technologies with modern artificial intelligence.",
          tags = listOf("intro", "bio", "about me")
        ),
        AnswerTemplate(
          id = "temp_2",
          userId = currentUser.value?.uid ?: "",
          title = "Career Goals & Motivation",
          category = "Academic",
          content = "My objective is to leverage software engineering and intelligent automation to solve real-world problems in education and workflow productivity.",
          tags = listOf("career", "goals", "motivation")
        )
      )
      for (t in defaultTemplates) {
        try {
          firestoreRepo.saveAnswerTemplate(t)
        } catch (_: Exception) {}
      }
    }
  }

  fun setWorkspaceTab(tab: Int) {
    _workspaceTab.value = tab
  }

  fun setAnswerMode(mode: AnswerMode) {
    _selectedMode.value = mode
  }

  fun setCustomInstructions(text: String) {
    _customInstructions.value = text
  }

  fun setAnswerLength(length: String) {
    _answerLength.value = length
  }

  fun setFormUrl(url: String) {
    _formUrl.value = GoogleFormsBridge.normalizeFormUrl(url)
  }

  // Load a rich sample Google Form with multi-page structure
  fun loadSampleDemoForm() {
    _currentSessionId.value = UUID.randomUUID().toString()
    _formTitle.value = "Tech Conference & Internship Survey 2026"
    _formUrl.value = "https://docs.google.com/forms/d/e/1FAIpQLSc-sample/viewform"
    _pageState.value = com.example.formpilot.model.FormPageState(
      currentPage = 1,
      totalPages = 2,
      pageIndicatorText = "Page 1 of 2",
      hasNextPage = true,
      hasPrevPage = false,
      isSubmitPage = false,
      sectionTitle = "Personal & Academic Information"
    )

    val page1Questions = listOf(
      FormQuestion(
        id = "demo_1",
        questionIndex = 0,
        pageIndex = 1,
        questionText = "Full Name",
        questionType = QuestionType.SHORT_ANSWER.name,
        isRequired = true,
        suggestedAnswer = "Kyle Airon Laroza",
        confidence = ConfidenceLevel.HIGH.name,
        reasoning = "Retrieved from user profile memory"
      ),
      FormQuestion(
        id = "demo_2",
        questionIndex = 1,
        pageIndex = 1,
        questionText = "School or Organization",
        questionType = QuestionType.SHORT_ANSWER.name,
        isRequired = true,
        suggestedAnswer = "State University of Technology",
        confidence = ConfidenceLevel.HIGH.name,
        reasoning = "Retrieved from user profile memory"
      ),
      FormQuestion(
        id = "demo_3",
        questionIndex = 2,
        pageIndex = 1,
        questionText = "What is your primary area of technical interest?",
        questionType = QuestionType.MULTIPLE_CHOICE.name,
        isRequired = true,
        options = listOf("Mobile App Development", "Machine Learning & AI", "Cybersecurity", "Cloud Infrastructure"),
        suggestedAnswer = "Mobile App Development",
        selectedChoices = listOf("Mobile App Development"),
        confidence = ConfidenceLevel.HIGH.name,
        reasoning = "Aligned with student background in Android and Kotlin"
      ),
      FormQuestion(
        id = "demo_4",
        questionIndex = 3,
        pageIndex = 1,
        questionText = "Which programming languages and platforms do you use regularly?",
        questionType = QuestionType.CHECKBOXES.name,
        isRequired = false,
        options = listOf("Kotlin", "Python", "TypeScript / JavaScript", "Rust", "Swift", "C++"),
        suggestedAnswer = "Kotlin, Python, TypeScript / JavaScript",
        selectedChoices = listOf("Kotlin", "Python", "TypeScript / JavaScript"),
        confidence = ConfidenceLevel.HIGH.name,
        reasoning = "Matched from supplied resume notes"
      )
    )
    _questions.value = page1Questions
    _pageQuestionsMap[1] = page1Questions
    _fillReport.value = null
    _consistencyReport.value = null
  }

  // Demo multi-page next
  fun demoNextPage() {
    val page2Questions = _pageQuestionsMap[2] ?: listOf(
      FormQuestion(
        id = "demo_p2_1",
        questionIndex = 4,
        pageIndex = 2,
        questionText = "Please describe a challenging project you built and how you solved unexpected obstacles.",
        questionType = QuestionType.PARAGRAPH.name,
        isRequired = true,
        suggestedAnswer = "During the development of FormPilot AI, I faced cross-origin constraints when inspecting Google Forms embedded in mobile browsers. To solve this, I designed a multi-tier hybrid architecture combining DOM JavaScript bridges for direct page inspection, Gemini multimodal vision for screenshot parsing, and a resilient manual review workflow so users maintain total control and verification.",
        confidence = ConfidenceLevel.HIGH.name,
        reasoning = "Generated in natural human tone matching project context"
      ),
      FormQuestion(
        id = "demo_p2_2",
        questionIndex = 5,
        pageIndex = 2,
        questionText = "How would you rate your proficiency with modern Android Jetpack Compose?",
        questionType = QuestionType.LINEAR_SCALE.name,
        isRequired = false,
        options = listOf("1", "2", "3", "4", "5"),
        suggestedAnswer = "5",
        selectedChoices = listOf("5"),
        confidence = ConfidenceLevel.HIGH.name,
        reasoning = "High proficiency demonstrated in codebase"
      ),
      FormQuestion(
        id = "demo_p2_3",
        questionIndex = 6,
        pageIndex = 2,
        questionText = "Expected Graduation Date",
        questionType = QuestionType.DATE.name,
        isRequired = false,
        suggestedAnswer = "2027-06-15",
        confidence = ConfidenceLevel.MEDIUM.name,
        reasoning = "Calculated from 3rd-year university standing"
      )
    )
    _pageState.value = com.example.formpilot.model.FormPageState(
      currentPage = 2,
      totalPages = 2,
      pageIndicatorText = "Page 2 of 2 (Final Page)",
      hasNextPage = false,
      hasPrevPage = true,
      isSubmitPage = true,
      sectionTitle = "Technical Experience & Goals"
    )
    _questions.value = page2Questions
    _pageQuestionsMap[2] = page2Questions
    _isNavigatingPage.value = false
    _pageNavigationMessage.value = "Advanced to Page 2: Technical Experience & Goals"
  }

  // Demo multi-page previous
  fun demoPrevPage() {
    loadSampleDemoForm()
    _isNavigatingPage.value = false
    _pageNavigationMessage.value = "Returned to Page 1: Personal & Academic Information"
  }

  fun setAutoAiOnPageChange(enabled: Boolean) {
    _autoAiOnPageChange.value = enabled
  }

  fun startNavigating() {
    _isNavigatingPage.value = true
    _pageNavigationMessage.value = null
  }

  fun onNavigationResult(action: String, success: Boolean, message: String) {
    _isNavigatingPage.value = false
    _pageNavigationMessage.value = message
  }

  fun clearNavigationMessage() {
    _pageNavigationMessage.value = null
  }

  // Receive questions extracted from Google Forms live WebView DOM with page state
  fun onQuestionsExtractedFromWeb(
    title: String,
    newPageState: com.example.formpilot.model.FormPageState,
    extractedQuestions: List<FormQuestion>
  ) {
    if (title.isNotBlank()) _formTitle.value = title

    val previousPage = _pageState.value.currentPage
    val isPageChanged = previousPage != newPageState.currentPage
    _pageState.value = newPageState

    if (extractedQuestions.isNotEmpty()) {
      // Retain previously answered values if the user navigates back and forth between pages
      val existingForThisPage = _pageQuestionsMap[newPageState.currentPage] ?: emptyList()
      val merged = extractedQuestions.map { freshQ ->
        val existing = existingForThisPage.find {
          it.questionText.trim().equals(freshQ.questionText.trim(), ignoreCase = true) ||
          it.questionIndex == freshQ.questionIndex
        }
        if (existing != null && (existing.suggestedAnswer.isNotBlank() || existing.selectedChoices.isNotEmpty())) {
          freshQ.copy(
            suggestedAnswer = existing.suggestedAnswer,
            selectedChoices = existing.selectedChoices,
            isLocked = existing.isLocked,
            isVerified = existing.isVerified,
            confidence = existing.confidence,
            reasoning = existing.reasoning
          )
        } else {
          freshQ
        }
      }

      _pageQuestionsMap[newPageState.currentPage] = merged
      _questions.value = merged
      saveCurrentDraftLocally()

      // When the page changes and auto AI is enabled, the AI automatically goes there and generates answers!
      val needsGeneration = merged.any { it.suggestedAnswer.isBlank() && it.selectedChoices.isEmpty() && !it.isLocked }
      if (_autoAiOnPageChange.value && needsGeneration) {
        generateAllAnswers()
      }
    }
    _isNavigatingPage.value = false
  }

  // Generate answers for all unlocked questions
  fun generateAllAnswers() {
    val qList = _questions.value
    if (qList.isEmpty()) return

    _isGenerating.value = true
    _generationError.value = null

    viewModelScope.launch {
      try {
        val updated = geminiService.generateAnswers(
          questions = qList,
          mode = _selectedMode.value,
          profile = _userProfile.value,
          referenceDocs = _referenceDocs.value,
          customInstructions = _customInstructions.value,
          answerLength = _answerLength.value
        )
        _questions.value = updated
        _isGenerating.value = false
        saveCurrentDraftLocally()
        // Save to cloud if authenticated
        if (currentUser.value != null) {
          saveCurrentDraftToCloud()
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error generating answers", e)
        _isGenerating.value = false
        _generationError.value = e.localizedMessage ?: "Answer generation failed. Please try again."
      }
    }
  }

  // Regenerate a single question
  fun regenerateSingleAnswer(questionId: String) {
    val qList = _questions.value
    val target = qList.find { it.id == questionId } ?: return

    _isGenerating.value = true
    _generationError.value = null

    viewModelScope.launch {
      try {
        val updatedList = geminiService.generateAnswers(
          questions = listOf(target.copy(isLocked = false)),
          mode = _selectedMode.value,
          profile = _userProfile.value,
          referenceDocs = _referenceDocs.value,
          customInstructions = _customInstructions.value,
          answerLength = _answerLength.value
        )
        val newTarget = updatedList.firstOrNull() ?: target
        _questions.value = qList.map { if (it.id == questionId) newTarget else it }
        _isGenerating.value = false
        saveCurrentDraftLocally()
      } catch (e: Exception) {
        _isGenerating.value = false
        _generationError.value = e.localizedMessage ?: "Failed to regenerate answer."
      }
    }
  }

  // Update text of an answer manually
  fun updateAnswer(questionId: String, newText: String) {
    _questions.value = _questions.value.map { q ->
      if (q.id == questionId) {
        q.copy(suggestedAnswer = newText, confidence = ConfidenceLevel.HIGH.name)
      } else q
    }
    saveCurrentDraftLocally()
  }

  // Toggle choices for multiple choice or checkboxes
  fun toggleChoice(questionId: String, choice: String) {
    _questions.value = _questions.value.map { q ->
      if (q.id == questionId) {
        val current = q.selectedChoices.toMutableList()
        val isRadio = q.getType() == QuestionType.MULTIPLE_CHOICE || q.getType() == QuestionType.LINEAR_SCALE || q.getType() == QuestionType.TRUE_FALSE
        if (isRadio) {
          q.copy(
            selectedChoices = listOf(choice),
            suggestedAnswer = choice,
            confidence = ConfidenceLevel.HIGH.name
          )
        } else {
          // Checkbox toggle
          if (current.contains(choice)) current.remove(choice) else current.add(choice)
          q.copy(
            selectedChoices = current,
            suggestedAnswer = current.joinToString(", "),
            confidence = ConfidenceLevel.HIGH.name
          )
        }
      } else q
    }
    saveCurrentDraftLocally()
  }

  // Lock answer
  fun toggleLock(questionId: String) {
    _questions.value = _questions.value.map { q ->
      if (q.id == questionId) q.copy(isLocked = !q.isLocked) else q
    }
  }

  // Mark answer verified
  fun toggleVerified(questionId: String) {
    _questions.value = _questions.value.map { q ->
      if (q.id == questionId) q.copy(isVerified = !q.isVerified) else q
    }
  }

  // Extract questions from screenshot using Gemini Multimodal Vision
  fun extractFromScreenshot(bitmap: Bitmap) {
    _isGenerating.value = true
    _generationError.value = null
    viewModelScope.launch {
      try {
        val (title, scannedQuestions) = geminiService.extractQuestionsFromScreenshot(bitmap)
        _formTitle.value = title
        _questions.value = scannedQuestions
        _isGenerating.value = false
        _workspaceTab.value = 1
        saveCurrentDraftLocally()
      } catch (e: Exception) {
        _isGenerating.value = false
        _generationError.value = "Screenshot extraction failed: ${e.localizedMessage}"
      }
    }
  }

  // Parse pasted text into questions
  fun extractFromPastedText(rawText: String) {
    if (rawText.isBlank()) return
    _isGenerating.value = true
    _generationError.value = null
    viewModelScope.launch {
      try {
        val (title, parsed) = geminiService.parsePastedQuestions(rawText)
        _formTitle.value = title
        _questions.value = parsed
        _isGenerating.value = false
        _workspaceTab.value = 1
        saveCurrentDraftLocally()
      } catch (e: Exception) {
        _isGenerating.value = false
        _generationError.value = "Text parsing failed: ${e.localizedMessage}"
      }
    }
  }

  // Auto-fill report callback from WebView
  fun onFillReportReceived(success: Int, failed: Int, msg: String) {
    _fillReport.value = FormFillReport(
      totalQuestions = _questions.value.size,
      filledCount = success,
      unverifiedCount = failed,
      details = listOf(msg)
    )
  }

  // Run form consistency & missing info analysis
  fun runConsistencyCheck() {
    _isAnalyzingConsistency.value = true
    viewModelScope.launch {
      val result = geminiService.analyzeFormSummaryAndConsistency(_questions.value)
      _consistencyReport.value = result
      _isAnalyzingConsistency.value = false
    }
  }

  // Save current draft to local Room DB
  fun saveCurrentDraftLocally() {
    viewModelScope.launch {
      val jsonArray = JSONArray()
      for (q in _questions.value) {
        val obj = JSONObject()
        obj.put("id", q.id)
        obj.put("questionIndex", q.questionIndex)
        obj.put("questionText", q.questionText)
        obj.put("questionType", q.questionType)
        obj.put("isRequired", q.isRequired)
        obj.put("options", JSONArray(q.options))
        obj.put("suggestedAnswer", q.suggestedAnswer)
        obj.put("selectedChoices", JSONArray(q.selectedChoices))
        obj.put("isLocked", q.isLocked)
        obj.put("isVerified", q.isVerified)
        obj.put("confidence", q.confidence)
        obj.put("reasoning", q.reasoning)
        jsonArray.put(obj)
      }

      val draft = DraftEntity(
        id = _currentSessionId.value,
        formUrl = _formUrl.value,
        formTitle = _formTitle.value,
        status = _formStatus.value,
        questionsJson = jsonArray.toString(),
        updatedAt = System.currentTimeMillis()
      )
      localDb.draftDao().insertOrUpdate(draft)
    }
  }

  // Save current draft to Cloud Firestore
  fun saveCurrentDraftToCloud() {
    val uid = currentUser.value?.uid ?: return
    viewModelScope.launch {
      try {
        val session = FormSession(
          id = _currentSessionId.value,
          userId = uid,
          formUrl = _formUrl.value,
          formTitle = _formTitle.value,
          detectedQuestionsCount = _questions.value.size,
          completedQuestionsCount = _questions.value.count { it.suggestedAnswer.isNotBlank() },
          status = _formStatus.value
        )
        firestoreRepo.saveFormSession(session, _questions.value)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to save draft to cloud", e)
      }
    }
  }

  // Resume saved draft from local Room
  fun resumeLocalDraft(draft: DraftEntity) {
    try {
      _currentSessionId.value = draft.id
      _formUrl.value = draft.formUrl
      _formTitle.value = draft.formTitle
      _formStatus.value = draft.status

      val array = JSONArray(draft.questionsJson)
      val list = mutableListOf<FormQuestion>()
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        val optArr = obj.optJSONArray("options") ?: JSONArray()
        val opts = (0 until optArr.length()).map { optArr.getString(it) }
        val chArr = obj.optJSONArray("selectedChoices") ?: JSONArray()
        val choices = (0 until chArr.length()).map { chArr.getString(it) }

        list.add(
          FormQuestion(
            id = obj.optString("id", "q_$i"),
            questionIndex = obj.optInt("questionIndex", i),
            questionText = obj.optString("questionText", ""),
            questionType = obj.optString("questionType", "SHORT_ANSWER"),
            isRequired = obj.optBoolean("isRequired", false),
            options = opts,
            suggestedAnswer = obj.optString("suggestedAnswer", ""),
            selectedChoices = choices,
            isLocked = obj.optBoolean("isLocked", false),
            isVerified = obj.optBoolean("isVerified", false),
            confidence = obj.optString("confidence", "HIGH"),
            reasoning = obj.optString("reasoning", "")
          )
        )
      }
      _questions.value = list
      _workspaceTab.value = 1
    } catch (e: Exception) {
      Log.e(TAG, "Failed to resume local draft", e)
    }
  }

  // Resume saved draft from Cloud Firestore
  fun resumeCloudDraft(session: FormSession) {
    viewModelScope.launch {
      try {
        _currentSessionId.value = session.id
        _formUrl.value = session.formUrl
        _formTitle.value = session.formTitle
        _formStatus.value = session.status
        val loaded = firestoreRepo.loadQuestionsForForm(session.id)
        _questions.value = loaded
        _workspaceTab.value = 1
      } catch (e: Exception) {
        Log.e(TAG, "Failed to resume cloud draft", e)
      }
    }
  }

  fun deleteLocalDraft(id: String) {
    viewModelScope.launch {
      localDb.draftDao().deleteDraftById(id)
    }
  }

  fun deleteCloudDraft(id: String) {
    viewModelScope.launch {
      try {
        firestoreRepo.deleteFormSession(id)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to delete cloud draft", e)
      }
    }
  }

  // Reference document actions
  fun addReferenceDocument(name: String, text: String) {
    val newDoc = ReferenceDocument(
      id = UUID.randomUUID().toString(),
      name = name,
      content = text,
      sizeKb = (text.toByteArray().size / 1024).coerceAtLeast(1),
      isSelected = true
    )
    _referenceDocs.value = _referenceDocs.value + newDoc
  }

  fun deleteReferenceDocument(id: String) {
    _referenceDocs.value = _referenceDocs.value.filter { it.id != id }
  }

  fun toggleReferenceDocSelection(id: String) {
    _referenceDocs.value = _referenceDocs.value.map {
      if (it.id == id) it.copy(isSelected = !it.isSelected) else it
    }
  }

  // Profile memory update
  fun updateProfile(profile: UserProfile) {
    _userProfile.value = profile
    viewModelScope.launch {
      try {
        firestoreRepo.saveUserProfile(profile)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to save profile", e)
      }
    }
  }

  // Add reusable template
  fun addTemplate(title: String, category: String, content: String, tags: List<String>) {
    val uid = currentUser.value?.uid ?: ""
    val template = AnswerTemplate(
      id = UUID.randomUUID().toString(),
      userId = uid,
      title = title,
      category = category,
      content = content,
      tags = tags
    )
    _templates.value = _templates.value + template
    if (uid.isNotBlank()) {
      viewModelScope.launch {
        try {
          firestoreRepo.saveAnswerTemplate(template)
        } catch (e: Exception) {
          Log.e(TAG, "Failed to save template to firestore", e)
        }
      }
    }
  }

  fun deleteTemplate(id: String) {
    _templates.value = _templates.value.filter { it.id != id }
    if (currentUser.value != null) {
      viewModelScope.launch {
        try {
          firestoreRepo.deleteAnswerTemplate(id)
        } catch (e: Exception) {
          Log.e(TAG, "Failed to delete template from firestore", e)
        }
      }
    }
  }
}
