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
| 2: letter shape comparison | `data/service/LetterShapeComparisonService`, `LocalImageShapeComparisonService`, and its unit test | `88e2847103df54569f15f99a055f911e78925880` (unchanged) | Scores how closely a drawn letter matches an admin-provided reference image of the letter, with plain Java image processing (no external service, no model). In the hackathon build it is combined with a Meta vision model reading the same drawing; the Meta part is new work |

## Scrubs applied to the snapshot (the only edits to pre-existing files)

| File | Change |
| --- | --- |
| `src/main/resources/application.properties` | Removed the default Google OAuth client id and secret, and the default production datasource URL. `DATABASE_URL` is now required from the environment. |

## Not in the baseline

The handwriting canvas and recognizer, mic recorder, persona and conversation code, letter and content services, admin and report views,
Azure OpenAI and Azure Speech services, seed data, images and audio are **not** here. Azure OpenAI and Azure Speech are not used in the
hackathon build at all. Any other existing module that the hackathon build reuses will be imported later as its own commit, titled
"Import pre-existing <module>", listed in this file with its source commit, and done before new code touches it.
