# docsuri-server

AI 기반 무역 서류(Invoice / B/L / Packing List) 정합성 검수 및 통관 지원 백엔드.

## 기술 스택

- Java 21, Spring Boot 4, Spring Data JPA, PostgreSQL 16, Gradle
- Google Gemini (LLM)

## 로컬 실행

### 1. PostgreSQL 준비

```bash
docker run --rm -d --name docsuri-pg \
  -e POSTGRES_DB=docsuri -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 postgres:16
```

### 2. 환경변수

| 변수 | 필수 | 기본값 | 설명 |
|---|---|---|---|
| `GEMINI_API_KEY` | **필수** | 없음 | Gemini API 키. 없으면 기동이 실패한다 |
| `DB_USERNAME` | 선택 | `postgres` | |
| `DB_PASSWORD` | 선택 | `postgres` | |
| `GEMINI_MODEL_REASONING` | 선택 | `gemini-2.5-pro` | HS Code 추천·소명, 정정 요청서 등 추론 비중이 큰 호출 |
| `GEMINI_MODEL_FAST` | 선택 | `gemini-2.5-flash` | 교차 검증 서술 등 빠른 응답이 필요한 호출 |
| `APP_OCR_PROVIDER` | 선택 | `gemini` | `gemini`(실제 OCR) 또는 `mock`(더미 데이터, 오프라인 개발/테스트용) |

### 3. 기동

매번 커맨드라인에 키를 직접 치는 대신 `.env` 파일을 쓸 수 있다. `.env`는 `.gitignore`에 등록되어 있어 커밋되지 않는다.

```bash
cp .env.example .env
# .env를 열어 GEMINI_API_KEY 등 값을 채운 뒤
./gradlew bootRun
```

(`build.gradle`의 `bootRun` 태스크가 `.env`를 자동으로 읽어 환경변수로 주입한다 — Spring Boot 자체는 `.env`를 읽지 않는다.)

또는 매번 직접 지정:

```bash
GEMINI_API_KEY=your-key ./gradlew bootRun
```

`ddl-auto: validate` + `spring.sql.init.mode: always` 조합이므로, 기동 시 `schema.sql`이 있는 스키마와 엔티티가 정확히 일치해야 하고 `data.sql`(면책문구/무역용어/HS Code 시드)이 매번 재적용된다.

## 테스트

```bash
# 로컬에 PostgreSQL(docsuri DB)이 떠 있어야 한다 — 위 1번 참고
GEMINI_API_KEY=dummy ./gradlew test
```

- `ValidationEngineTest`: Java 결정론 검증 로직 단위 테스트. `GeminiClient`에 의존하지 않으므로, 여기서 통과하면 "LLM을 스텁으로 바꿔도 등급/카운트/신호등이 동일하다"는 것이 구조적으로 보장된다.
- `CorrectionRequestExportServiceTest`: PDF/DOCX에 한글이 실제로 깨지지 않는지, 생성된 파일을 다시 읽어(PDFBox/POI) 확인한다.
- `FullFlowIntegrationTest`: 업로드 → 파싱(mock OCR) → 검증(`GeminiClient` 스텁) → 정정요청 → PATCH → export(PDF) 전체 플로우를 실제 HTTP 요청으로 검증한다.

## 설계서와의 편차

`DECISIONS.md`에 설계서(`00-CONVENTIONS.md` ~ `05-TASKS.md`)와 실제 구현이 갈라진 지점과 사유를 기록해 두었다. 가장 중요한 편차는 OCR — 설계서는 이번 단계에 인터페이스만 두라고 했지만, 개발 속도를 위해 `GeminiOcrClient`(실제 Gemini 호출)를 처음부터 활성 구현체로 유지했다.

## 도메인 구조

```
common/      공통 응답, 예외, Enum
config/      AsyncConfig, WebConfig
ai/          GeminiClient, PromptTemplate, 도메인별 responseSchema
guide/       무역 용어 사전, 면책 문구
document/    업로드, OCR 파싱
validation/  교차 검증 (Java 결정론 + LLM 서술)
hscode/      HS Code 추천·상세·소명
correction/  정정 요청서 생성/수정/export
```
