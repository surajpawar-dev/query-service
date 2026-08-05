# RAG Query Service

Spring Boot service for the final user-facing RAG query flow. It accepts a user question, searches relevant document chunks through `rag-embedding-service`, builds a grounded prompt, streams the prompt to an LLM, and returns a ChatGPT-style streaming response to the caller.

## Responsibility

This service owns:

- User question API.
- Retrieval orchestration through `rag-embedding-service`.
- Prompt construction from retrieved chunks.
- Streaming LLM answer generation.
- Returning source chunks for citations.

It does not upload files, parse PDFs, generate document chunks, create embeddings, or write vectors directly.

## Runtime Flow

```text
Client
  -> rag-query-service
      -> rag-embedding-service /api/v1/embeddings/search
      -> Ollama /api/chat
      -> Server-Sent Events response
```

Recommended ownership:

- `rag-embedding-service` owns vector search and OpenSearch index details.
- `rag-query-service` owns prompt orchestration and LLM answer streaming.

## Local Setup

The parent `document-rag-platform/docker-compose.yml` runs this service with:

```text
SPRING_PROFILES_ACTIVE=local
```

Local dependencies:

- `rag-embedding-service` at `http://rag-embedding-service:8080` inside Docker.
- Ollama at `http://ollama:11434` inside Docker.
- Local LLM model pulled by the compose file.

For standalone local runs outside Docker:

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
$env:EMBEDDING_SERVICE_BASE_URL = "http://localhost:8082"
$env:LLM_BASE_URL = "http://localhost:11434"
$env:LLM_MODEL = "llama3.1"
.\mvnw.cmd spring-boot:run
```

## Higher Environment Infrastructure

For dev, staging, or production:

```text
SPRING_PROFILES_ACTIVE=prod
```

Required infrastructure/configuration:

```text
EMBEDDING_SERVICE_BASE_URL=https://embedding.internal.example.com
LLM_PROVIDER=ollama
LLM_BASE_URL=https://ollama-prod.internal.example.com
LLM_MODEL=llama3.1
SECURITY_JWT_ENABLED=true
```

When you move to AWS Bedrock, Azure OpenAI, OpenAI, or another cloud LLM, keep the same service boundary and add a new `LlmClient` implementation behind the existing interface. The public query API does not need to change.

Production notes:

- Use internal networking between query, embedding, and LLM services.
- Enable JWT validation for shared environments.
- Do not log raw user questions with sensitive data unless your logging policy allows it.
- Keep answer prompts grounded in retrieved chunks only.
- Monitor LLM latency, retrieval latency, stream failures, and no-context responses.

## Configuration

| Environment variable | Default | Purpose |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `local` | Runtime profile. |
| `SERVER_PORT` | `8080` | HTTP port inside the container. |
| `EMBEDDING_SERVICE_BASE_URL` | `http://localhost:8082` | Base URL for `rag-embedding-service`. |
| `QUERY_DEFAULT_TOP_K` | `5` | Default number of vector matches. |
| `QUERY_MAX_QUESTION_LENGTH` | `2000` | Max accepted question length. |
| `QUERY_INCLUDE_SOURCES_BY_DEFAULT` | `true` | Sends source chunks after answer tokens. |
| `LLM_PROVIDER` | `ollama` | LLM provider selector. |
| `LLM_BASE_URL` | `http://localhost:11434` | Ollama-compatible API base URL. |
| `LLM_MODEL` | `llama3.1` | Chat model used to answer. |
| `LLM_TEMPERATURE` | `0.2` | Answer generation temperature. |
| `LLM_MAX_CONTEXT_CHARACTERS` | `12000` | Max retrieved context inserted into prompt. |
| `SECURITY_JWT_ENABLED` | `false` | Enables JWT auth in higher environments. |

## API

### Stream Answer

```http
POST /api/v1/query/stream
Content-Type: application/json
Accept: text/event-stream
```

Request:

```json
{
  "question": "What are the refund rules?",
  "topK": 5,
  "documentIds": ["bde09e5d-608d-43ad-9048-6dce424fcad0"],
  "includeSources": true
}
```

Streaming events:

```text
event: token
data: {"type":"token","content":"Refunds","sources":null}

event: sources
data: {"type":"sources","content":null,"sources":[...]}

event: done
data: {"type":"done","content":null,"sources":null}
```

## Build And Test

```powershell
.\mvnw.cmd spotless:apply
.\mvnw.cmd spotless:check
.\mvnw.cmd test
```

## Docker

```powershell
docker build -t rag-query-service:latest .
docker run --rm -p 8083:8080 rag-query-service:latest
```
