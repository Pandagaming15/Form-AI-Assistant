package com.example.formpilot.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.example.formpilot.model.AnswerTemplate
import com.example.formpilot.model.FormQuestion
import com.example.formpilot.model.FormSession
import com.example.formpilot.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val TAG = "FirestoreRepo"

class FirestoreRepository(
  private val db: FirebaseFirestore
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google to access cloud data.")
  }

  // --- USER PROFILE ---
  fun observeUserProfile(): Flow<UserProfile?> = flow {
    val uid = auth.currentUser?.uid ?: return@flow
    emitAll(
      db.collection("users").document(uid)
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(UserProfile::class.java)
          } else {
            null
          }
        }
    )
  }

  suspend fun saveUserProfile(profile: UserProfile) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    val snapshot = docRef.get().await()

    val payload = hashMapOf<String, Any?>(
      "userId" to uid,
      "displayName" to profile.displayName,
      "email" to profile.email,
      "photoUrl" to profile.photoUrl,
      "schoolOrOrg" to profile.schoolOrOrg,
      "department" to profile.department,
      "preferredTone" to profile.preferredTone,
      "preferredLanguage" to profile.preferredLanguage,
      "notes" to profile.notes,
      "memoryEnabled" to profile.memoryEnabled,
      "updatedAt" to FieldValue.serverTimestamp()
    )

    if (!snapshot.exists()) {
      payload["createdAt"] = FieldValue.serverTimestamp()
    }

    docRef.set(payload.filterValues { it != null }).await()
    Log.d(TAG, "User profile saved for $uid")
  }

  // --- FORM SESSIONS / DRAFTS ---
  fun observeFormSessions(): Flow<List<FormSession>> = flow {
    val uid = requireUserId()
    emitAll(
      db.collection("users").document(uid).collection("forms")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(FormSession::class.java).sortedByDescending {
            it.updatedAt?.seconds ?: 0L
          }
        }
    )
  }

  suspend fun saveFormSession(session: FormSession, questions: List<FormQuestion>) {
    val uid = requireUserId()
    val formId = if (session.id.isBlank()) {
      db.collection("users").document(uid).collection("forms").document().id
    } else {
      session.id
    }
    session.id = formId
    session.userId = uid

    val formDocRef = db.collection("users").document(uid).collection("forms").document(formId)
    val existing = formDocRef.get().await()

    val formPayload = hashMapOf<String, Any?>(
      "id" to formId,
      "userId" to uid,
      "formUrl" to session.formUrl,
      "formTitle" to session.formTitle,
      "detectedQuestionsCount" to questions.size,
      "completedQuestionsCount" to questions.count { it.suggestedAnswer.isNotBlank() || it.selectedChoices.isNotEmpty() },
      "status" to session.status,
      "updatedAt" to FieldValue.serverTimestamp()
    )

    if (!existing.exists()) {
      formPayload["createdAt"] = FieldValue.serverTimestamp()
    }

    formDocRef.set(formPayload.filterValues { it != null }).await()

    // Save individual questions
    val questionsCol = formDocRef.collection("questions")
    for (q in questions) {
      val qId = if (q.id.isBlank()) questionsCol.document().id else q.id
      q.id = qId
      q.formId = formId
      val qPayload = hashMapOf<String, Any?>(
        "id" to qId,
        "formId" to formId,
        "questionIndex" to q.questionIndex,
        "questionText" to q.questionText,
        "questionType" to q.questionType,
        "isRequired" to q.isRequired,
        "options" to q.options,
        "suggestedAnswer" to q.suggestedAnswer,
        "selectedChoices" to q.selectedChoices,
        "isLocked" to q.isLocked,
        "isVerified" to q.isVerified,
        "confidence" to q.confidence,
        "reasoning" to q.reasoning,
        "updatedAt" to FieldValue.serverTimestamp()
      )
      questionsCol.document(qId).set(qPayload.filterValues { it != null }).await()
    }
    Log.d(TAG, "Saved form session $formId with ${questions.size} questions")
  }

  suspend fun loadQuestionsForForm(formId: String): List<FormQuestion> {
    val uid = requireUserId()
    val snapshot = db.collection("users").document(uid).collection("forms").document(formId)
      .collection("questions")
      .get()
      .await()
    return snapshot.toObjects(FormQuestion::class.java).sortedBy { it.questionIndex }
  }

  suspend fun deleteFormSession(formId: String) {
    val uid = requireUserId()
    val formRef = db.collection("users").document(uid).collection("forms").document(formId)
    // delete questions subcollection first
    val questions = formRef.collection("questions").get().await()
    for (doc in questions.documents) {
      doc.reference.delete().await()
    }
    formRef.delete().await()
    Log.d(TAG, "Deleted form session $formId")
  }

  // --- ANSWER TEMPLATES LIBRARY ---
  fun observeAnswerTemplates(): Flow<List<AnswerTemplate>> = flow {
    val uid = requireUserId()
    emitAll(
      db.collection("users").document(uid).collection("templates")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(AnswerTemplate::class.java).sortedByDescending {
            it.updatedAt?.seconds ?: 0L
          }
        }
    )
  }

  suspend fun saveAnswerTemplate(template: AnswerTemplate) {
    val uid = requireUserId()
    val templateId = if (template.id.isBlank()) {
      db.collection("users").document(uid).collection("templates").document().id
    } else {
      template.id
    }
    template.id = templateId
    template.userId = uid

    val docRef = db.collection("users").document(uid).collection("templates").document(templateId)
    val existing = docRef.get().await()

    val payload = hashMapOf<String, Any?>(
      "id" to templateId,
      "userId" to uid,
      "title" to template.title,
      "category" to template.category,
      "content" to template.content,
      "tags" to template.tags,
      "updatedAt" to FieldValue.serverTimestamp()
    )

    if (!existing.exists()) {
      payload["createdAt"] = FieldValue.serverTimestamp()
    }

    docRef.set(payload.filterValues { it != null }).await()
    Log.d(TAG, "Saved template $templateId")
  }

  suspend fun deleteAnswerTemplate(templateId: String) {
    val uid = requireUserId()
    db.collection("users").document(uid).collection("templates").document(templateId).delete().await()
  }
}
