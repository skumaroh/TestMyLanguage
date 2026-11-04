# New in the hackathon

TestMyLanguage is a new application, built during the Meta Global AI Developer Hackathon (Oct 27 to Nov 4, 2026), with the Meta Model API
doing as much of the work as possible: Muse Spark writes, judges, watches and summarises, and Muse Voice Transcribe listens.
The small amount of earlier code and content it builds on is listed in `PRE_EXISTING.md` (the first commit, tagged `pre-hackathon-baseline`).
`git diff pre-hackathon-baseline..main` shows exactly what is new. Only a handful of earlier files were edited, and every edit is listed in `PRE_EXISTING.md`.

**How it was built.** The code was written by the author with the help of an AI coding assistant (Claude Code, by Anthropic), which co-authored the commits (see the `Co-Authored-By` lines). The author directed the work, tested the live apps and reviewed and merged every pull request.

## The product in one paragraph
TestMyLanguage places a learner of Tamil or Hindi at Level 1 in about fifteen minutes. A Meta-written proctor welcomes them; six parts follow (words, letters, listening, translation, conversation, video); the score stays hidden until a final report. Each part uses the same rule: 4 questions, 4 more if under 75%, at most 8. The level passes at 75% overall. Two live apps (Tamil, Hindi) run on the same code with separate databases.

## Where Meta is used
| Piece | Meta product | How |
| --- | --- | --- |
| Proctor texts, translation grading, spelling suggestions, conversation (persona lines and grading), clip analysis, report summary, conversation generation | Muse Spark 1.3 | Meta Model API, JSON-schema output |
| Watching the video clips and drafting the expected answers | Muse Spark 1.3 video input | Meta Model API (`video_url`) |
| Spoken answers | Muse Voice Transcribe | Meta Model API `/asr/transcribe` |
| Listen buttons | MMS-TTS open-source models | Own service in the cluster (see Voice) |

## Not Meta (labelled as such in the product and the demo)
- Browser speech synthesis: the fallback voice of the Listen buttons when the speech service is unreachable.
- The letter-shape comparison (earlier code, see `PRE_EXISTING.md`), which scores handwritten letters. Meta vision reading of drawings was tried and measured as unreliable; it is **not** part of any score and survives only as an optional tick-box on the admin test page (`HandwritingReader`, `HandwritingCheckView`).
- The video clips: made outside the API in a Meta Muse chat session (Meta's Model API has no video generation); Muse Spark then watches them through the API. They are stored in Azure Blob Storage, not in this repository.
- Development tooling: Claude Code (see above).

## Step 1: runnable skeleton and deployment path
| Item | What it is |
| --- | --- |
| `HomeView` (route `/`) | Signed-in landing page. Shows the user and language, and checks PostgreSQL and Blob Storage reachability |
| `SecurityConfig` change | Google sign-in is enabled only when the environment supplies a Google client; health probe paths and the media and speech routes are configured |
| `LoginView` change | No "OAuth providers not configured" warning when only email and password are available |
| `AzureBlobService` change | Added `containerExists`, a cheap list-only health check that works with a read-and-list token |
| `config/BlobStorageHealthIndicator` | Blob Storage health component for the readiness endpoint |
| `application.properties` | Slimmed to the settings this app uses; no defaults for database or Google credentials |
| `build.gradle`, `settings.gradle`, `Dockerfile` | Project renamed to TestMyLanguage; Spring Boot Actuator added for health probes; libraries for excluded features removed |
| `.github/workflows/build-push-ghcr.yml` | Builds the app image, runs the tests, pushes to GitHub Container Registry and triggers the rollout of the live apps |
| `.env.example`, `README.md`, this file | Documentation |

## Task 1: Meta-written proctor texts
| Item | What it is |
| --- | --- |
| `ai/` (`MetaAiGateway`, `AiRequest`, `AiResult`, `AiCallLog`) | The only code that calls the Meta Model API; every call is logged without prompts, answers or keys |
| `proctor/` | Text service, validator, prompt, built-in fallbacks and background warm-up for the greeting and the activity introductions |
| `WelcomeView`, `ProctorReviewView` | The welcome page (Listen button) and the admin page to approve, edit or discard texts |

## The assessment engine and admin tools
| Item | What it is |
| --- | --- |
| `assessment/` (`BlockEngine`, `LevelScore`, `AssessmentService`, `AssessAttempt`, `AssessBlockResult`, `AssessAnswer` and repositories) | The block rule (4, then 4 more, 75%), the level rule, and storage of attempts, block results and every answer |
| `AssessWord`, `AssessLetters` | **Read-only views** of the word and letter content the author prepared earlier (see `PRE_EXISTING.md`) |
| `MainLayout`, `AdminBlocksView` ("Test Blocks"), `AdminFeedback` | The menu, and the admin page that runs any one part directly and shows Correct or Wrong after each answer (students never see it) |
| `AssessmentMediaService`, `MediaUrls`, `views/api/MediaController` | Serves word pictures, letter and word audio and clips from private Blob containers through a signed-in-only route |

## The six parts
| Part | Code | Notes |
| --- | --- | --- |
| 1 Words | `WordsBlockView`, `WordQuestionService`, `TileBuilder`, `WordQuestion` | Build a word from letter tiles, with its picture or sound |
| 2 Writing letters | `DrawLettersBlockView`, `handwriting/` (`DrawingPad`, `HandwritingJudge`, `ShapeRanking`, `LetterPicker`, `ReferenceLetter`, `ReferenceImageService`, `LetterCandidate`), `drawing-pad.js` | The drawn letter is ranked against the level's letters with the pre-existing shape comparison; no Meta in the score |
| 3 Listening | `AudioWordsBlockView` | Hear a word, build it |
| 4 Translation | `translation/`, `TranslationBlockView`, `TranslationCheckView`, `transliterate.js`, `SmartAnswerBox` | Half word tiles, half typed with live English-letters-to-script conversion (own rule-based converter); Meta judges meaning, suggests spellings word by word, and reports spelling separately |
| 5 Conversation | `conversation/`, `ConversationBlockView`, `ConversationAdminView`, `IdleGuard`, `idle-guard.js` | Meta generates imagined everyday conversations as drafts, an admin verifies them, a learner never gets the same one twice; Meta plays the persona and judges each reply; a quiet period closes it |
| 6 Video | `video/`, `ClipAdminView`, `VideoBlockView` | Meta watches each clip and drafts the English description, the answers in Tamil or Hindi, variants and a clarity verdict; an admin verifies; the learner says what is happening |

## Voice: speaking and listening
| Item | What it is |
| --- | --- |
| `ai/MetaSpeechToText`, `voice/VoiceInput`, `voice-recorder.js` | The learner speaks an answer; the browser records a short clip and Muse Voice Transcribe turns it into text. Audio is sent to Meta and is not stored here |
| `tts/` | A small speech service built on Meta's open-source MMS-TTS models (`facebook/mms-tts-tam`, `facebook/mms-tts-hin`), downloaded unmodified from Hugging Face when the image is built. **Licence: CC-BY-NC 4.0 (non-commercial)**; to be reviewed before any commercial use |
| `ai/MmsTextToSpeech`, `voice/SpeechRegistry`, `views/api/TtsController`, `SpeakButton`, `speak.js` | The Listen buttons play that voice; lines are looked up by a short key so text never appears in a web address. If the service is unreachable the button uses the browser's own voice |
| `.github/workflows/build-push-tts.yml` | Builds the speech service image only when `tts/` changes |

## Final report and history
| Item | What it is |
| --- | --- |
| `report/` (`ReportService`, `ReportMath`, `AnswerLog`, `ReportSnapshot`, `ReportSummaryWriter`) | Completes an attempt (totals, percent, level result at 75% overall), builds the report, stores a snapshot of it, and asks Muse Spark for a short written summary from the computed numbers only |
| `ReportView`, `HistoryView` | The student's results page and the list of past results |

## Tables
`assess_proctor_text`, `assess_ai_call_log` (V082); `assess_attempt`, `assess_block_result`, `assess_answer` (V083); read grants on existing content tables (V084, V085); `assess_conversation`, `assess_conversation_objective` (V086); `assess_report` (V087); `assess_video_clip` (V088).

## Content created during the hackathon
| Content | Who made it |
| --- | --- |
| `src/main/resources/translation/level1-sentences.txt` (24 English sentences and their Tamil and Hindi answers) | Written with Claude Code; the Tamil and Hindi are **drafts awaiting native review** |
| Proctor texts, conversations (personas, lines, objectives), clip descriptions and answers, report summaries | Written by Meta Muse Spark through the Model API; conversations, clips and proctor texts are reviewed and verified by an admin before use; report summaries are generated per attempt and stored |
| Video clips | Made in a Meta Muse chat; stored in Blob Storage, not in this repository |

## Third-party components added during the hackathon
| Component | Where | Licence |
| --- | --- | --- |
| Spring Boot Actuator | `build.gradle` | Apache-2.0 |
| jsPDF, html2canvas (npm; make the certificate PDF in the browser) | `CertificateView`, `certificate-pdf.js` | MIT, MIT |
| qrcode-generator (npm; draws the QR code on the certificate) | `CertificateView`, `certificate-qr.js` | MIT |
| PyTorch (CPU), Hugging Face Transformers, NumPy | `tts/` (speech service image only) | BSD-style, Apache-2.0, BSD-3 |
| `facebook/mms-tts-tam`, `facebook/mms-tts-hin` model weights | downloaded when the speech image is built | **CC-BY-NC 4.0** (non-commercial) |
The Java and front-end dependencies of the application shell (Spring Boot, Vaadin, Azure SDK, PostgreSQL driver) come from the earlier code and are unchanged apart from the build changes above. The only new npm packages are jsPDF, html2canvas and qrcode-generator, listed above.

## Known gaps
Level 1 only. The Tamil and Hindi answers in the sentence list, and the Tamil and Hindi lines written by Meta for conversations and clips, are checked by the author and still need a native speaker's read. Speech in and out is still being checked with real voices. The MMS voice licence must be reviewed before any commercial use. Meta's MMS voices are single-speaker male voices, so every conversation persona is male; adding female and age-varied voices needs a multi-speaker voice model and is planned after the hackathon.
