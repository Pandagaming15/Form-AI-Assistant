package com.example.formpilot.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "AuthManager"

class AuthManager(private val context: Context) {
  private val auth: FirebaseAuth = Firebase.auth
  private val credentialManager = CredentialManager.create(context)

  private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
  val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  init {
    auth.addAuthStateListener { firebaseAuth ->
      _currentUser.value = firebaseAuth.currentUser
    }
  }

  fun clearError() {
    _errorMessage.value = null
  }

  // Silent Auto-Sign-In on App Startup
  fun attemptAutoSignIn(scope: CoroutineScope) {
    if (auth.currentUser != null) {
      _currentUser.value = auth.currentUser
      return
    }

    val clientId = try {
      context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      Log.w(TAG, "default_web_client_id string not found: ${e.message}")
      return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()

    val request = GetCredentialRequest.Builder()
      .addCredentialOption(googleIdOption)
      .build()

    scope.launch {
      try {
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
          val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
          val authResult = auth.signInWithCredential(authCredential).await()
          _currentUser.value = authResult.user
          Log.d(TAG, "Silent auto-sign-in succeeded for ${authResult.user?.email}")
        }
      } catch (e: Exception) {
        // Silent failure is normal for first time or logged out users
        Log.d(TAG, "Silent sign-in not available: ${e.message}")
      }
    }
  }

  // Interactive Sign-In with Google using Jetpack CredentialManager
  fun signInWithGoogle(activity: Activity, scope: CoroutineScope, onSuccess: () -> Unit = {}) {
    val clientId = try {
      context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      _errorMessage.value = "Google configuration missing: default_web_client_id"
      return
    }

    _isLoading.value = true
    _errorMessage.value = null

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(signInOption)
      .build()

    scope.launch {
      try {
        val result = credentialManager.getCredential(activity, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
          val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
          val authResult = auth.signInWithCredential(authCredential).await()
          _currentUser.value = authResult.user
          _isLoading.value = false
          onSuccess()
          Log.d(TAG, "Interactive Google sign-in succeeded: ${authResult.user?.email}")
        } else {
          _isLoading.value = false
          _errorMessage.value = "Unexpected credential format returned."
        }
      } catch (e: GetCredentialCancellationException) {
        Log.w(TAG, "Google Sign-In flow was cancelled by the user: ${e.message}")
        _isLoading.value = false
      } catch (e: Exception) {
        Log.e(TAG, "Google Sign-In failed", e)
        _isLoading.value = false
        _errorMessage.value = e.localizedMessage ?: "Sign in failed. Please try again."
      }
    }
  }

  // Sign out of Firebase and clear Credential Manager state
  fun signOut(scope: CoroutineScope, onComplete: () -> Unit = {}) {
    auth.signOut()
    _currentUser.value = null
    scope.launch {
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.e(TAG, "Failed to clear credential state", e)
      } finally {
        onComplete()
      }
    }
  }
}
