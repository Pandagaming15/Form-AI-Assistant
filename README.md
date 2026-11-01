# FormPilot AI — Smart Google Forms Assistant

**FormPilot AI** is a production-grade Android application engineered with Kotlin, Jetpack Compose, Material 3, and Google Gemini AI. It transforms the Google Forms completion workflow into an intelligent, guided, and automated experience.

---

## 🌟 Key Features

1. **Google Forms Workspace & Hybrid Extraction**
   - **In-App Form WebView**: Loads any Google Forms link (`forms.google.com`, `forms.gle`) inside an interactive browser with safe navigation controls.
   - **DOM JavaScript Inspection**: Automatic extraction of form title, question prompts, types, required flags, and choice options directly from the live page.
   - **Auto-Fill Bridge**: One-click injection of AI-crafted answers directly into matching input, textarea, radio, and checkbox form fields.
   - **Chrome Handoff**: Quick button to open restricted forms (e.g., Google Workspace single-sign-on or CAPTCHA-protected forms) directly in the system browser.

2. **Gemini 3.8 Flash AI Answering Engine**
   - **Diverse Answering Modes**:
     - *Quick Answer*: Concise, high-speed responses.
     - *Detailed Answer*: Comprehensive rationales and academic explanations.
     - *Natural Human Writing*: Authentic, conversational tone.
     - *Formal Academic*: Professional and scholarly vocabulary.
     - *Simple English*: Accessible, clear vocabulary.
     - *Filipino / Taglish*: Conversational Tagalog and Taglish.
   - **Strict Type-Aware Answering**:
     - Multiple choice: strictly selects from existing valid choices.
     - Checkboxes: returns valid choice subsets.
     - Paragraphs: well-structured responses matching desired length (concise, standard, detailed).
     - Scale/Rating: selects valid integers within the scale.
     - Date & Time: formatted dates and timestamps.

3. **Multimodal Vision OCR & Raw Text Parser**
   - **Screenshot Question Reader**: Upload or capture a screenshot of any Google Form; Gemini Multimodal Vision reads the image and converts questions into interactive fields.
   - **Raw Text Parser**: Paste messy copied form text or exam prompts and convert them instantly into an active form session.

4. **Google Identity & Credential Manager**
   - Official Google Sign-In with Jetpack `CredentialManager` (`GetSignInWithGoogleOption`).
   - Real-time profile indicator with avatar, display name, email, and connection status.
   - Transparent identity notice explaining the difference between the FormPilot AI account and the browser respondent session.
   - One-tap account switching and secure sign-out.

5. **Personal Profile & Reusable Answer Library**
   - Profile memory: preferred name, university/organization, major/department, personal context notes.
   - Reusable Answer Templates: library of user-approved answers (e.g., self-introduction, career motivation, background summaries).
   - Non-sensitive data only; passwords and credentials are never stored.

6. **Two-Tier Persistence (Offline Room + Cloud Firestore)**
   - **Offline Drafts**: Room database stores draft forms and answers locally so you can work without an internet connection.
   - **Cloud Sync**: Firebase Firestore synchronizes active drafts and answer templates across devices.

7. **Review & Human-in-the-Loop Safety**
   - Pre-submission review screen highlighting completion percentage, missing required fields, and items flagged for clarification.
   - Single question regeneration, manual text editing, and answer locking.
   - AI Consistency & Conflict Checker identifying contradictory answers across the form.
   - Manual submission compliance: FormPilot never bypasses Google security or submits without explicit user action.

---

## 🛠️ Architecture & Tech Stack

| Layer | Technologies |
|---|---|
| **UI Framework** | Jetpack Compose, Material 3, Edge-to-Edge (`enableEdgeToEdge`), WindowInsets safeDrawing |
| **Language & Platform** | Kotlin 2.2, Android SDK 36 (targetSdk 36, minSdk 24) |
| **AI Integration** | Firebase AI Logic (`gemini-3.8-flash`) with App Check attestation |
| **Authentication** | Jetpack `CredentialManager`, `googleid`, Firebase Auth (`GetSignInWithGoogleOption`) |
| **Cloud Storage** | Firebase Firestore Enterprise Edition with ABAC security rules (`firestore.rules`) |
| **Local Storage** | Room Database (SQLite) with KSP compiler |
| **Image Loading** | Coil Compose |
| **CI/CD** | GitHub Actions (`.github/workflows/build-apk.yml`) |

---

## 🚀 Building & Running

### Local Development Build
To compile and assemble the debug APK locally:
```bash
gradle assembleDebug
```
The generated APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### Running Unit Tests
```bash
gradle :app:testDebugUnitTest
```

### GitHub Actions CI/CD APK Generation
The included workflow `.github/workflows/build-apk.yml` triggers automatically on pushes to `main` or manual workflow dispatch, building the debug APK and uploading it as a downloadable artifact.

---

## 🔒 Security & Privacy Notice

- FormPilot AI never requests or stores Google account passwords.
- All Firebase Firestore operations are gated behind Google Sign-In and enforced by zero-trust security rules (`firestore.rules`).
- The application does not defeat one-response limits, organizational restrictions, or CAPTCHA protections.
- Users always retain final review of all generated answers before manually submitting forms.
