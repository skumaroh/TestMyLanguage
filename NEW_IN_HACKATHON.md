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

## Planned (not yet built)

Assessment engine and state machine, five activities, Meta model gateway (Muse Spark, Muse Voice Transcribe), semantic grading,
persona conversation, video comprehension, final report, history and teacher view. This file is updated as each lands.

## Not Meta

Anything not produced by a Meta model or the Meta Model API is labeled as such here and in the demo video, including browser speech
synthesis for the proctor's voice and the video clips.
