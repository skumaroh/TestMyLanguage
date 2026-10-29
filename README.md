# TestMyLanguage

AI-guided Tamil and Hindi placement assessments, built for the Meta Global AI Developer Hackathon.

One Spring Boot and Vaadin codebase, deployed twice (`test-my-tamil.mylangskills.com` and `test-my-hindi.mylangskills.com`),
with username and password sign-up, email verification, PostgreSQL and Azure Blob Storage.

**Status:** step 1 (skeleton and deployment path). See `NEW_IN_HACKATHON.md` for what is new and `PRE_EXISTING.md` for what
this repository started from.

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

The pre-existing code is released under the Unlicense (see `LICENSE.md`).
