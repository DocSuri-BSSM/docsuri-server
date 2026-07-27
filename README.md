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

## Docker / 배포 (AWS)

로컬 Gradle 없이 애플리케이션 + PostgreSQL을 한 번에 띄운다. `Dockerfile`은 멀티스테이지 빌드(Gradle JDK 이미지로 빌드 → JRE 이미지로 실행)이고, `/actuator/health`를 헬스체크로 사용한다.

### 로컬에서 docker compose로 확인

```bash
cp .env.example .env   # GEMINI_API_KEY 등 채우기
docker compose up -d --build
curl http://localhost:8080/actuator/health
```

`docker-compose.yml`은 같은 디렉터리의 `.env`를 자동으로 읽는다 (Gradle의 `.env` 로딩과는 별개로, docker compose 자체 기능). 업로드 파일/export 결과는 `app-storage` 볼륨에, DB 데이터는 `db-data` 볼륨에 저장되어 컨테이너를 내렸다 올려도 유지된다.

```bash
docker compose logs -f app   # 로그 확인
docker compose down          # 컨테이너만 내리기 (볼륨 유지)
docker compose down -v       # 볼륨까지 완전히 삭제
```

### AWS EC2에 배포하기

가장 단순한 구성은 EC2 인스턴스 하나에 Docker + Docker Compose를 설치하고 이 리포지토리를 그대로 올려서 실행하는 것이다.

1. **EC2 준비**: Amazon Linux 2023 또는 Ubuntu, 최소 t3.small 이상 권장 (Gradle 빌드가 인스턴스 안에서 도는 경우 메모리를 꽤 먹는다 — 아래처럼 로컬에서 이미지를 빌드해 푸시하는 방식이면 t3.micro도 가능).
2. **보안 그룹**: 인바운드 8080(또는 ALB/Nginx를 앞에 둘 경우 443/80만) 허용. 5432(Postgres)는 외부에 열지 않는다 — `docker-compose.yml`에서 `db` 서비스는 `ports`를 노출하지 않고 내부 네트워크로만 접근 가능하게 되어 있다.
3. **Docker 설치 후 배포**:
   ```bash
   git clone <repo-url> && cd docsuri-server
   cp .env.example .env   # 실제 GEMINI_API_KEY 등으로 채움 (절대 git에 커밋하지 않는다)
   docker compose up -d --build
   ```
4. **HTTPS/도메인**: 이 프로젝트 자체는 TLS를 처리하지 않는다. EC2 앞에 ALB(+ ACM 인증서) 또는 Nginx/Caddy 리버스 프록시를 두고 8080으로 전달하는 구성을 권장한다.
5. **이미지 미리 빌드해서 배포하는 방식** (EC2에서 직접 빌드하지 않고 싶을 때): 로컬 또는 CI에서 `docker build -t <ECR-repo-uri>:latest .` 후 ECR에 푸시하고, EC2/`docker-compose.yml`의 `app.build: .`를 `app.image: <ECR-repo-uri>:latest`로 바꿔 `docker compose pull && docker compose up -d`만 실행하면 된다.

**운영 시 유의할 점**:
- 파일 저장이 `app-storage` 볼륨(로컬 디스크)에 의존하므로, 인스턴스를 여러 대로 스케일아웃하면 파일 접근이 깨진다. 지금 구조는 단일 인스턴스 배포를 전제로 한다 (S3 전환은 `FileStorage` 인터페이스 뒤에서 구현체만 교체하면 됨 — `DECISIONS.md` 참고).
- `.env`의 `GEMINI_API_KEY`는 EC2에서도 절대 git에 올리지 말고, 가능하면 AWS Secrets Manager/Parameter Store에서 가져와 배포 스크립트가 `.env`를 생성하도록 하는 것을 권장한다.
- `app.gemini.model.reasoning`이 무료 티어에서 쿼터 문제로 막힐 수 있다 (실제로 이 프로젝트 테스트 중 발생) — 배포 전 Google AI Studio에서 결제 활성화 여부를 확인할 것.

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
