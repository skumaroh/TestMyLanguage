# Database tables added by this application

The application's own tables are named `assess_*` and live in the `public` schema of each language's database, next to the
tables of the existing LanguageTool app. **The migration scripts are not in this repository.** All database changes live in the
private `LanguageToolDBScripts` repository (migration `V082`), run through its existing pipeline; all deployment settings live in
the private `langtool-deploy` repository. This repository holds the application code only.

| Table | Purpose |
| --- | --- |
| `assess_proctor_text` | The greeting and activity introductions written by a Meta model. Each text is a candidate (`PENDING`) until an admin approves (`APPROVED`) or discards (`DISCARDED`) it; students only see approved texts. Columns: purpose, activity, level, proctor language, body, model, prompt version, source (`META` or `EDITED`), status, reviewer, timestamps |
| `assess_ai_call_log` | One row per model call: purpose, model, status, HTTP status, latency, token counts, and a short error type. It never stores a prompt, an answer or a key |

The application reads the existing `"user"` table (sign-up, sign-in, the `ADMIN` role) and, later, the existing content tables. It uses
a dedicated database login (`assessment_app_user`) that has only the rights these tables and that read access need.
