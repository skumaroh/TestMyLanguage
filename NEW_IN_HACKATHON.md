# New in the hackathon

Everything below was written during the Meta Global AI Developer Hackathon (Oct 27 to Nov 4, 2026).
The pre-existing code is listed in `PRE_EXISTING.md` and is the first commit, tagged `pre-hackathon-baseline`.
`git diff pre-hackathon-baseline..main` shows exactly what is new.

## Step 1: runnable skeleton and deployment path

| Item | What it is |
| --- | --- |
| `HomeView` (route `/`) | Signed-in landing page. Shows the user and language, and checks PostgreSQL and Blob Storage reachability |
| `SecurityConfig` change | Google sign-in is enabled only when the environment supplies a Google client; health probe paths are public |
| `LoginView` change | No "OAuth providers not configured" warning when only email and password are available |
| `application.properties` | Slimmed to the settings this app uses; no defaults for database or Google credentials |
| `build.gradle` | Spring Boot Actuator added for health probes; libraries for excluded features removed |
| `.github/workflows/build-push-ghcr.yml` | Builds the image and pushes it to GitHub Container Registry |
| `.env.example`, `README.md`, this file | Documentation |

## Task 1: Meta-written proctor texts

| Item | What it is |
| --- | --- |
| `ai/` (`MetaAiGateway`) | The only code that calls the Meta Model API (Muse Spark); every call is logged without prompts, answers or keys |
| `proctor/` | Text service, validator, prompt, built-in fallbacks and background warm-up for the greeting and five activity introductions |
| `WelcomeView`, `ProctorReviewView` | The student's welcome page (with a browser-voice Listen button, labeled not Meta) and the admin page to approve, edit or discard texts |
| Tables | `assess_proctor_text`, `assess_ai_call_log`: see `docs/DATABASE.md`. Migrations are in the private `LanguageToolDBScripts` repository |

## Voice: speaking and listening

| Item | What it is |
| --- | --- |
| `ai/MetaSpeechToText`, `voice/VoiceInput`, `voice-recorder.js` | The learner speaks an answer; the browser records a short clip and Meta speech-to-text (Muse Voice Transcribe) turns it into text. Audio is sent to Meta and is not stored here |
| `tts/` | A small speech service built on Meta's open-source MMS-TTS models (`facebook/mms-tts-tam`, `facebook/mms-tts-hin`), downloaded unmodified from Hugging Face when the image is built. **Licence: CC-BY-NC 4.0 (non-commercial)**; to be reviewed before any commercial use |
| `ai/MmsTextToSpeech`, `voice/SpeechRegistry`, `views/api/TtsController`, `SpeakButton` | The Listen buttons play that voice; lines are looked up by a short key so text never appears in a web address. If the service is unreachable the button uses the browser's own voice |

## Final report and history

| Item | What it is |
| --- | --- |
| `report/` (`ReportService`, `ReportMath`, `AnswerLog`, `ReportSnapshot`, `ReportSummaryWriter`) | Completes an attempt (totals, percent, level result at 75% overall), builds the report, stores a snapshot of it, and asks Meta (Muse Spark) for a short written summary from the computed numbers only |
| `ReportView`, `HistoryView` | The student's results page (level result, points per part, summary, every question with the expected answer and notes) and the list of past results |
| Table | `assess_report` (stored report snapshot and summary), migration V087 in the private `LanguageToolDBScripts` repository |

## Planned (not yet built)

Assessment engine and state machine, five activities, Meta model gateway (Muse Spark, Muse Voice Transcribe), semantic grading,
persona conversation, video comprehension, final report, history and teacher view. This file is updated as each lands.

## Not Meta

Anything not produced by a Meta model or the Meta Model API is labeled as such here and in the demo video, including browser speech
synthesis (the proctor's voice and the fallback for Listen buttons) and the video clips.
