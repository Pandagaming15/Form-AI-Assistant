
package com.example.formpilot.auth

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "AppCheckInit"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

fun configureAppCheck(context: Context) {
    try {
        val firebaseApp = FirebaseApp.getApps(context)
            .firstOrNull() ?: FirebaseApp.initializeApp(context)

        if (firebaseApp == null) {
            Log.e(TAG, "Firebase initialization failed.")
            return
        }

        val appCheck = FirebaseAppCheck.getInstance(firebaseApp)

        if (BuildConfig.DEBUG) {
            appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )

            Log.d(TAG, "Debug App Check provider installed.")
        } else {
            val resId = context.resources.getIdentifier(
                RECAPTCHA_SITE_KEY_RES,
                "string",
                context.packageName
            )

            if (resId != 0) {
                val siteKey = context.getString(resId)

                if (siteKey.isNotBlank()) {
                    appCheck.installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                    Log.d(TAG, "Release App Check provider installed.")
                } else {
                    Log.e(TAG, "reCAPTCHA site key is blank.")
                }
            } else {
                Log.e(TAG, "reCAPTCHA site key resource not found.")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "App Check initialization failed.", e)
    }
}
