# AI Legal Document Assistant

A Spring Boot (Java 17+) backend that lets a user upload a legal document
(PDF, DOCX, or TXT) and automatically get:

- A **plain-English summary**
- **Document type** classification (NDA, Lease, Employment, Contract, etc.)
- **Key clause extraction** (Termination, Confidentiality, Indemnification,
  Liability, Governing Law, Payment Terms, IP, Non-Compete, Dispute
  Resolution, Force Majeure)
- A **risk flag** (LOW / MEDIUM / HIGH) with supporting notes
- A **follow-up Q&A** endpoint to ask questions about the uploaded document

It ships with a tiny built-in web page (`/`) so you can try it without Postman.

> ⚠️ This is an educational/portfolio project. It does not provide legal
> advice and should not be relied on for real legal decisions.

## Two modes, zero required setup

- **Offline mode (default):** no API key needed. Summaries, risk review, and
  Q&A are produced by a deterministic rule-based analyzer (keyword/regex
  matching over clause types and common risk phrases like "auto-renew",
  "sole discretion", "unlimited liability", missing termination/governing-law
  clauses, etc). The whole app runs and demos correctly out of the box.
- **AI mode:** set an API key (see below) and the same endpoints instead call
  a real LLM (any OpenAI-compatible `/chat/completions` endpoint) for
  noticeably better summaries, risk reasoning, and open-ended Q&A. The service
  automatically falls back to offline mode if the call fails for any reason
  (bad key, network, quota), so it never hard-crashes a request.

## Requirements

- Java 17+ (project was built/tested on Java 21)
- Maven 3.8+
- No database server needed — uses an embedded, file-based H2 database
  (`./data/legalai`), created automatically on first run.

## Run it

```bash
mvn spring-boot:run
# or
mvn clean package -DskipTests
java -jar target/ai-legal-document-assistant-1.0.0.jar
```

Then open **http://localhost:8080** in a browser for the built-in UI, or use
the REST API directly (see below). The H2 console is available at
**http://localhost:8080/h2-console** (JDBC URL: `jdbc:h2:file:./data/legalai`,
user `sa`, empty password) if you want to inspect stored data.

## Enabling real AI mode

Set an environment variable before starting the app:

```bash
export LLM_API_KEY=sk-...your-key...
# optional overrides:
export LLM_API_URL=https://api.openai.com/v1/chat/completions
export LLM_MODEL=gpt-4o-mini
java -jar target/ai-legal-document-assistant-1.0.0.jar
```

Any OpenAI-compatible provider works — just point `LLM_API_URL` and
`LLM_MODEL` at it (e.g. a local Ollama/LM Studio server, Azure OpenAI,
OpenRouter, etc).

## REST API

| Method | Path                        | Description                                   |
|--------|-----------------------------|------------------------------------------------|
| POST   | `/api/documents/upload`     | Multipart upload (`file`), returns full analysis |
| GET    | `/api/documents`            | List all analyzed documents                    |
| GET    | `/api/documents/{id}`       | Get one document's analysis                    |
| DELETE | `/api/documents/{id}`       | Delete a document                              |
| POST   | `/api/documents/{id}/ask`   | Body: `{"question": "..."}` — ask about it     |

### Example

```bash
curl -X POST -F "file=@nda_sample.pdf" http://localhost:8080/api/documents/upload

curl -X POST http://localhost:8080/api/documents/1/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "Can either party terminate this early?"}'
```

## Project structure

```
src/main/java/com/legalai/
  LegalAiApplication.java        Entry point
  controller/DocumentController  REST endpoints
  service/
    TextExtractionService        PDF/DOCX/TXT -> plain text (PDFBox, POI)
    ClauseExtractionService      Offline keyword/regex clause finder
    AIAnalysisService            Summary / risk / Q&A, LLM or offline
    DocumentService              Orchestrates the pipeline + persistence
  model/LegalDocument.java       JPA entity
  repository/                   Spring Data JPA repository
  dto/                           API request/response shapes
  exception/GlobalExceptionHandler
src/main/resources/
  application.properties
  static/index.html              Minimal demo UI
src/test/java/...                Unit test for clause extraction
```

## Notes / limitations (be upfront about these if presenting this project)

- The offline analyzer is a heuristic, not real comprehension — it is meant
  to make the app fully runnable/demoable without any external API key, and
  as a fallback if the LLM call fails.
- Clause detection works on paragraph/sentence windows and keyword matches;
  it can miss non-standard phrasing or attribute a clause to the wrong
  section in unusually formatted documents.
- File size capped at 15MB by default (`application.properties`).
- Not legal advice, and not a substitute for review by a licensed attorney.

## Possible extensions

- True RAG: chunk + embed the document and retrieve top-k chunks per
  question instead of sending the whole document to the LLM.
- Multi-document comparison (e.g., redline two contract versions).
- Clause library with "market standard" language to compare against.
- User accounts + document history per user.
- Export analysis as a PDF/Word report.
