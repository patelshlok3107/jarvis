# JARVIS — Shlok's AI Assistant (Android + Cloud)

> **Mobile-only, background-first, never a desktop app.** Android APK handles real cellular calls. Cloud (Vercel + Render) hosts the responsive web UI — no localhost, no PC required.

## Deployment

| Layer | Host | URL | Status |
|-------|------|-----|--------|
| Frontend | Vercel | `https://jarvis-frontend.vercel.app` (set via Vercel dashboard) | `npm run build` ✓ |
| Backend | Render | `https://jarvis-backend.onrender.com` | `/health` → `{"status":"ok"}` ✓ |
| Android | Device | APK `app/build/outputs/apk/debug/app-debug.apk` | Real Telecom |

**Architecture**

```
Phone (browser) -> Vercel (Next.js 14, frontend/) -> Render (Express, backend/) -> AI
Phone (native) -> Android Telecom (CallScreeningService / ConnectionService) -> JARVIS
```

## Architecture (spec §14-15)

```
Mobile App (Jetpack Compose, dark futuristic UI)
   ↓
JarvisEngine (status → decision, contact rules)
   ↓
AiProvider interface → TemplateAiProvider (offline) / CloudAiProvider (OpenAI/Gemini stub, key in EncryptedSharedPreferences)
   ↓
TTS (TextToSpeech) + STT (SpeechRecognizer) — Hindi/Gujarati/English
   ↓
Telecom: CallScreeningService + RoleManager.ROLE_CALL_SCREENING / ROLE_DIALER
ForegroundService (phoneCall|microphone) + BootReceiver + QS Tile
Secure storage: DataStore + EncryptedSharedPreferences
```

## REAL vs SIMULATED — honest per spec §20

Android **forbids** third-party apps from answering a cellular call and injecting TTS audio into the telephony stream nor capturing caller audio **unless** the app holds `RoleManager.ROLE_DIALER` (Default Phone app) and uses `ConnectionService`. This is a carrier/OS restriction, not a bug.

- **Without Default Dialer:** `CallScreeningService` can only `silence / reject / block / log`. JARVIS does that, writes a `CallHistoryEntry(isSimulated=true)`, speaks the would-be response on the *phone speaker* (not into the call), and posts a high-priority notification "Rahul called while busy — tap to call back". History shows `[SIMULATED]` and Onboarding explains how to Fix.
- **With Default Dialer (user taps Fix → Settings):** scaffolding is ready to escalate to `ConnectionService` answering — `isDefaultDialer()` gate in `JarvisCallScreeningService:65`.

Never faked as "Call answered" when the call still rings.

## Quick Start

### 1. Cloud (no localhost)

**Frontend — Vercel**

1. Push repo to GitHub
2. Vercel → New Project → import repo → Root Directory `frontend` → add env `NEXT_PUBLIC_API_URL=https://your-backend.onrender.com` → Deploy
3. Frontend build: `next build` (5.25kB, static) — verified ✓

**Backend — Render**

1. Render → New Web Service → connect repo → Root `backend` → Build `npm install` → Start `npm start` → add env `ALLOWED_ORIGIN=https://your-frontend.vercel.app`, `AI_API_KEY` (optional) → Deploy
2. Health: `GET https://your-backend.onrender.com/health` → `{"status":"ok"}` — verified ✓
3. CORS: only Vercel origin allowed (not `*`)

`.env.example` at root / `frontend/.env.example` / `backend/.env.example` — never commit secrets.

### 2. Android APK (real calls)

Requirements: Android Studio Hedgehog+, JDK 17, SDK 34.

```powershell
.\gradlew assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
adb install app/build/outputs/apk/debug/app-debug.apk
```

Open in Android Studio → Sync → Run on device (minSdk 26, target 34).

## Phases implemented

1. **UI + Status system** — `JarvisStatus` (6 states), `JarvisCore` glowing animation, status chips.
2. **Permissions + Background** — `PermissionManager`, `JarvisForegroundService` (START_STICKY, notification `JARVIS — ● BUSY`), `BootReceiver`, `JarvisTileService` (cycle AVAILABLE→BUSY→DND).
3. **Call handling** — `JarvisCallScreeningService` (silence/reject + history + notification), `JarvisEngine.decide()` with contact/unknown/spam rules.
4. **Voice** — `SttManager` (tap-to-talk, explains wake-word battery cost), `TtsManager`, `VoiceCommandProcessor` ("I'm busy" → BUSY, "who called", etc.).
5. **AI** — `AiProvider` interface + `TemplateAiProvider` (concise, no hard-coded single line) + `CloudAiProvider` stub (endpoint/key from EncryptedStore).
6. **Rules** — `CallRulesScreen` (Mom/Rahul Always Allow, Unknown → JARVIS_HANDLES/ALLOW/BLOCK).
7. **History** — `HistoryRepository` (DataStore JSON, encrypted transcripts via `EncryptedStore`), `HistoryScreen` + `NotificationHelper`.
8. **Battery** — guidance + `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, OEM tips.

## Permissions onboarding

`Home → ℹ` shows each permission, `Call Screening Role`, `Default Dialer` REAL vs SIMULATED card, Battery Optimization fix, Refresh.

## Voice examples

- "Jarvis, I'm busy." → BUSY + "Understood, Shlok. I'll handle your incoming calls."
- "Jarvis, I'm available." → AVAILABLE
- "Jarvis, don't disturb me for one hour." → DND (60 min)
- "Jarvis, who called me?" / "show missed calls" → opens activity

## Settings

General (user/Jarvis name), Call Assistant templates (busy/meeting/sleeping/driving/DND), Voice (speed/pitch/lang en/hi/gu, preview), AI Provider, Privacy (recording OFF by default, encrypted, never uploaded without consent, clear history), Background.

## File map

- `app/src/main/AndroidManifest.xml` — permissions, FG service, CallScreeningService, Tile, BootReceiver
- `data/JarvisStatus.kt` — statuses, rules, history model, templates
- `engine/JarvisEngine.kt` — decision + AI providers
- `service/*` — FG service, screening, boot, notifications
- `voice/*` — TTS/STT/commands
- `ui/screens/*` — Home, Onboarding, History, Rules, Settings + `ui/components/JarvisCore.kt`
- `storage/*` — DataStore + EncryptedSharedPreferences

## Legal

Recording/transcript OFF by default. Respects `POST_NOTIFICATIONS`, `READ_PHONE_STATE`, `RoleManager`. Complies with call-recording laws — user must enable explicitly.

---
Built Android-first. iOS port would use CallKit + PushKit (more restricted — documented similarly).
