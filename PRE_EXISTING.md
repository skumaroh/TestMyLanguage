# Pre-existing code in this baseline

This repository starts from a **scrubbed, no-history snapshot** of the author's existing LanguageTool application
(private repository `skumaroh/LanguageTool`, commit `88e2847103df54569f15f99a055f911e78925880`). The first commit is tagged `pre-hackathon-baseline`.
`git diff pre-hackathon-baseline..main` shows exactly the work done during the Meta Global AI Developer Hackathon.

License of the pre-existing code: Unlicense (public domain), see `LICENSE.md`.

## What the baseline contains (tranche 1: accounts and email)

| Module | Files | Purpose |
| --- | --- | --- |
| Application shell | `Application`, `config/*` | Spring Boot and Vaadin setup, language configuration |
| Security | `security/*` | Spring Security username and password login, session audit, Google OAuth handler (not enabled in the hackathon build) |
| Users and sessions | `data/entity/User*`, `Status`, `Teacher`, `UserSessionLog`, token entities, matching repositories | Account storage |
| Registration and email | `UserRegistrationService`, `EmailVerificationService`, `PasswordResetService`, `EmailService` | Sign-up, email verification, password reset (SendGrid) |
| Views | `LoginView`, `RegistrationView`, `EmailVerificationView`, `ForgotPasswordView`, `ResetPasswordView` | Account screens |
| Storage | `AzureBlobService` | Azure Blob Storage access |
| Build and runtime | Gradle files, `Dockerfile`, Vaadin theme, `.env.example` | Build and packaging |

## Later imports (each its own commit, done before new code uses the module)

| Tranche | Files | Source commit | Purpose |
| --- | --- | --- | --- |
| 2: letter shape comparison | `data/service/LetterShapeComparisonService`, `LocalImageShapeComparisonService`, and its unit test | `88e2847103df54569f15f99a055f911e78925880` (unchanged) | Scores how closely a drawn letter matches an admin-provided reference image of the letter, with plain Java image processing (no external service, no model). In the hackathon build it ranks a drawing against the letters of the level and is the only thing that scores handwriting; the ranking logic around it is new work. Verified byte-for-byte identical to the source commit |

## Edits to pre-existing files (the only ones; everything else in the baseline is untouched)

| File | Change |
| --- | --- |
| `src/main/resources/application.properties` | Removed the default Google OAuth client id and secret, and the default production datasource URL (scrub); later slimmed to the settings this app uses and extended with the hackathon settings |
| `SecurityConfig`, `LoginView` | Google sign-in only when configured; no "providers not configured" warning; routes for media and speech |
| `AzureBlobService` | Added `containerExists` (list-only health check) |
| `build.gradle`, `settings.gradle`, `Dockerfile` | Project renamed; Actuator added; libraries for excluded features removed (image thumbnails, MP3 encoder, circuit breaker) |
| `.env.example`, `PRE_EXISTING.md` | Documentation |

## Pre-existing data and media (not code) that the hackathon build reads

The apps read content that belongs to the author's existing LanguageTool product and was **not** created during the hackathon:
words and their pictures and audio (the existing `asset` and `letters` tables and the image and audio containers in Azure Blob Storage), the reference letter images (`writing_letter`, letter-reference container), and the author's recordings of letters. They are read only (the apps hold read-only database and storage rights) and are not part of this repository, except the logo image, which is in the baseline.

## Not in the baseline

The handwriting canvas and judge, the voice recorder, the persona and conversation code, the word, translation and video parts, the
admin and report views and the speech service were **all written new during the hackathon**; see `NEW_IN_HACKATHON.md`. Azure OpenAI and
Azure Speech services from the author's product are not used in the hackathon build at all. Any other existing module that the build
reuses is imported as its own commit, titled "Import pre-existing <module>", listed above with its source commit, and done before new
code touches it.
