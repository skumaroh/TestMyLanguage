# Earlier code and content that this app builds on

TestMyLanguage is a new application, written for the Meta Global AI Developer Hackathon. Before the hackathon the author had
experimented, alone, with a few separate pieces in different places: an account and sign-in shell, a letter-shape comparison, and
some language content (words, pictures, audio). None of it was in official production use by anyone; the author was the only person
experimenting with it. The pieces this app reuses are listed here so that it is clear what is new and what is not.

The first commit of this repository is a clean snapshot of that earlier code, tagged `pre-hackathon-baseline`.
`git diff pre-hackathon-baseline..main` shows exactly the work done during the hackathon.

License of the earlier code: Unlicense (public domain), see `LICENSE.md`.

## What the first commit contains (accounts and email)

| Module | Files | Purpose |
| --- | --- | --- |
| Application shell | `Application`, `config/*` | Spring Boot and Vaadin setup, language configuration |
| Security | `security/*` | Spring Security username and password login, session audit, Google OAuth handler (not enabled in the hackathon build) |
| Users and sessions | `data/entity/User*`, `Status`, `Teacher`, `UserSessionLog`, token entities, matching repositories | Account storage |
| Registration and email | `UserRegistrationService`, `EmailVerificationService`, `PasswordResetService`, `EmailService` | Sign-up, email verification, password reset (SendGrid) |
| Views | `LoginView`, `RegistrationView`, `EmailVerificationView`, `ForgotPasswordView`, `ResetPasswordView` | Account screens |
| Storage | `AzureBlobService` | Azure Blob Storage access |
| Build and runtime | Gradle files, `Dockerfile`, Vaadin theme, `.env.example` | Build and packaging |

## Later import (its own commit, made before new code uses it)

| Module | Files | Purpose |
| --- | --- | --- |
| Letter shape comparison | `data/service/LetterShapeComparisonService`, `LocalImageShapeComparisonService`, and its unit test (unchanged) | Scores how closely a drawn letter matches a reference image of the letter, with plain Java image processing (no external service, no model). In the hackathon build it ranks a drawing against the letters of the level and is what scores handwriting; the ranking logic around it is new work |

## Edits to those files (the only ones; everything else in the first commit is untouched)

| File | Change |
| --- | --- |
| `src/main/resources/application.properties` | Removed the default Google OAuth client id and secret and the default datasource URL; slimmed to the settings this app uses and extended with the hackathon settings |
| `SecurityConfig`, `LoginView` | Google sign-in only when configured; no "providers not configured" warning; routes for media and speech |
| `AzureBlobService` | Added `containerExists` (list-only health check) |
| `build.gradle`, `settings.gradle`, `Dockerfile` | Project renamed; Actuator added; libraries for features this app does not use removed |
| `.env.example`, this file | Documentation |

## Content prepared earlier (data and media, not code) that the app reads

The app reads language content that the author prepared before the hackathon: words with their pictures and audio (the `asset` and
`letters` tables and the image and audio containers in Azure Blob Storage), reference letter images (`writing_letter` and the
letter-reference container), and the author's own recordings of letters. The app only reads this content (it holds read-only
database and storage rights). It is not part of this repository, except the logo image.

## Written new during the hackathon

Everything else: the handwriting canvas and judge, the voice recorder, the persona and conversation code, the word, listening,
translation and video parts, the admin and report views, the Meta Model API integration and the speech service. See
`NEW_IN_HACKATHON.md`.
