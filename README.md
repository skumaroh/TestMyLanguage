# TestMyLanguage

AI-guided Tamil and Hindi placement assessments, a new application built for the Meta Global AI Developer Hackathon with the
Meta Model API (Muse Spark and Muse Voice Transcribe) doing as much of the work as possible.

One Spring Boot and Vaadin codebase, deployed twice (`test-my-tamil.mylangskills.com` and `test-my-hindi.mylangskills.com`),
with username and password sign-up, email verification, PostgreSQL and Azure Blob Storage.

**Status:** all six parts of the Level 1 assessment, the final report and the admin tools are built. See `NEW_IN_HACKATHON.md`
for what is new and `PRE_EXISTING.md` for the small amount of earlier code and content it builds on.

## Run locally

Requirements: JDK 21, Node.js 20, a reachable PostgreSQL database.

```bash
cp .env.example .env      # fill in DATABASE_URL and any optional values
./gradlew bootRun
```

Open http://localhost:8080. The app reads configuration from environment variables only; no credentials are stored in this
repository.

## Container image

`.github/workflows/build-push-ghcr.yml` builds the production jar and pushes
`ghcr.io/skumaroh/testmylanguage:<commit-sha>` on every push to `main`.

## License

The earlier code is released under the Unlicense (see `LICENSE.md`).
