# Contributing

Thanks for taking a look. This is a personal project, but issues and pull requests are welcome.

## Getting set up

The full local setup is in the [README](README.md#getting-started-local). In short: start the
pgvector database with Docker, run the Spring Boot backend, and run the Vite dev server for the
frontend. You need Java 21, Node 20+, Docker, and at least one free LLM API key.

## Before opening a pull request

Please make sure both sides still build and pass:

```bash
cd backend  && mvn test        # backend unit tests
cd frontend && npm run build   # type-check + production build
```

CI runs these on every push and pull request, so a green check locally means a green check on
GitHub.

## A few conventions

- Keep changes focused. Small, single-purpose PRs are easier to review.
- Match the style already in the file you are editing.
- Backend DTOs and their TypeScript counterparts in `frontend/src/lib/types.ts` should stay in
  sync. If you change one, change the other.
- Don't commit secrets. API keys go in a local `.env` (already gitignored); see `.env.example`.

## Reporting a bug

Open an issue with what you did, what you expected, and what actually happened. If it's about a
debate failing, the error message in the UI and the backend log usually say why.
