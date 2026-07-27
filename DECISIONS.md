# 설계서와의 편차 기록

`05-TASKS.md` "판단이 필요할 때" 절차에 따라, 설계서와 모순되는 진행 사항을 여기에 기록한다.

## 1. OCR — 실제 엔진(Gemini)을 이번 단계부터 사용

**설계서 규정** (`04-AI-INTEGRATION.md` 7절): "이번 구현에서 실제 OCR 엔진은 붙이지 않습니다. 인터페이스만 정의하십시오." — `UnavailableOcrClient`를 `@ConditionalOnMissingBean`으로 두고, 실구현 없이 진행하도록 명시.

**실제 진행**: `document/service/GeminiOcrClient`가 Gemini 멀티모달 API를 직접 호출해 문서 이미지에서 필드를 추출하는 실구현체로 T1 시점부터 존재하며 기본 활성 상태다. `UnavailableOcrClient`는 만들지 않았다.

**사유**: 개발 속도를 위해 사용자가 명시적으로 결정 — 별도 OCR 엔진(Google Vision 등) 연동 전에 Gemini로 우선 동작하는 파이프라인을 먼저 확보하기 위함.

**후속 영향**:
- (해결됨, T4) `@Profile`이 아닌 `@ConditionalOnProperty(name = "app.ocr.provider", ...)`로 구현체를 선택하도록 이관 완료. `OcrClient`/`OcrRequest` 시그니처도 설계서 형태(`byte[] content`, `contentType`, `documentType`, `ocrLanguage`, `extractFields`)로 교체했다.
- OCR 정확도(특히 숫자 인식)에 대한 책임이 전통 OCR 엔진이 아닌 LLM에 있으므로, T6 Validation 도메인에서 판정에 사용하는 값의 신뢰도를 낮게 볼 여지가 있다. 필요 시 재검토.

## 2. Validation — 의미 비교(LLM) 규칙을 문자열 정규화로 단순화

**설계서 규정** (`03-FEATURES.md` 3.3절): `DESCRIPTION_MATCH`, `PARTY_NAME_MATCH`, `PORT_MATCH`, `ADDRESS_MATCH`는 "Java가 완전 일치 아님까지만 판정하고, 같은 뜻인지는 LLM에게 물어 WARNING/ERROR를 결정"하도록 되어 있다. 그런데 `04-AI-INTEGRATION.md` 3절의 정본 `responseSchema`(`rule/title/subtitle/cause/risk_warning`)에는 LLM이 등급을 되돌려줄 필드 자체가 없고, "LLM이 status를 바꿔 보내도 무시하고 Java 판정을 씁니다"라고 명시돼 있어 두 절이 서로 모순된다.

**실제 진행**: 네 규칙 모두 Java에서 공백/대소문자/특수문자 정규화 후 문자열 비교만 수행하고, 불일치 시 Tier 기본값(Tier1=ERROR, Tier2=WARNING)을 그대로 사용한다. "Pusan vs Busan" 같은 의미 동일성 판단은 하지 않는다.

**사유**: 두 절의 모순 중 더 구체적이고 나중에 나온(3절 뒤에 온) 정본 스키마 규칙("LLM 응답의 status는 신뢰하지 않는다")을 우선시했다. 별도의 의미비교 전용 LLM 호출을 새로 설계하는 대신 기존 스키마와 일관되게 단순화했다.

## 3. HS Code — ADDRESS_MATCH용 필드 키를 임의로 정의

`01-ERD.md` 3절의 `extract_fields` 예시에는 `SHIPPER_NAME`/`CONSIGNEE_NAME`은 있지만 주소 관련 필드 키가 없다. `ADDRESS_MATCH` 규칙이 동작하려면 필드 키가 필요하므로, `SHIPPER_ADDRESS`/`CONSIGNEE_ADDRESS`를 추가로 정의해 `ValidationEngine`에서 사용했다. 실제 프론트/OCR 프롬프트에서 이 키 이름으로 데이터를 채우지 않으면 이 규칙은 항상 "비교 불가"로 건너뛰어진다.

## 4. Correction Export — 한글 폰트를 NotoSansKR 대신 NanumBarunGothic으로 대체

**설계서 규정** (`02-API.md` 15번 엔드포인트): "`resources/fonts/`에 NotoSansKR을 넣고 템플릿 CSS에서 `@font-face`로 등록하십시오."

**실제 진행**: 개발 환경에 NotoSansKR 파일이 없어, 로컬 macOS에 이미 설치되어 있던 `NanumBarunGothic.ttf`(네이버 배포, SIL Open Font License — 재배포 가능한 오픈소스 폰트)를 `src/main/resources/fonts/`에 넣고 openhtmltopdf의 `useFont()`로 프로그래밍 방식 임베딩했다 (CSS `@font-face` 대신 Java 코드에서 폰트 패밀리를 등록하고 CSS는 `font-family` 이름만 참조).

**사유**: 핵심 요구사항은 "PDF에서 한글이 깨지지 않는다"는 기능이지 특정 폰트 파일 자체가 아니라고 판단. `CorrectionRequestExportServiceTest`에서 실제 PDF를 PDFBox로 다시 읽어 한글 텍스트가 정확히 추출되는지 검증했다.
**후속 조치 필요**: 실제 배포 시 라이선스/디자인 가이드에 맞는 폰트(NotoSansKR 등)로 교체할 것.
