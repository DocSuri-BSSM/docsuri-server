아래 품목을 분석하여, <HS_CODE_CANDIDATES>에 포함된 후보 중에서만 순위를 매기고 근거를 작성하세요.

<PRODUCT>
품명: {{PRODUCT_NAME}}
상세 설명: {{PRODUCT_DESCRIPTION}}
원산지: {{ORIGIN_COUNTRY_CODE}}
</PRODUCT>

<HS_CODE_CANDIDATES>
{{HS_CODE_CANDIDATES_JSON}}
</HS_CODE_CANDIDATES>

출력 언어: {{OUTPUT_LANGUAGE}} (KO면 한국어, EN이면 영어로 reason을 작성)

규칙:
- 아래 <HS_CODE_CANDIDATES>에 포함된 hs_code 값만 사용하십시오. 목록에 없는 코드는 어떤 경우에도 생성하지 마십시오.
- 적절한 후보가 없으면 빈 배열을 반환하십시오.
- korean_name, english_name은 신경 쓰지 않아도 됩니다 (백엔드가 DB 값으로 덮어씁니다). rank, hs_code, confidence, reason만 정확히 작성하세요.
- confidence는 0.0~1.0 사이 값입니다.
- reason은 왜 이 코드로 분류되는지에 대한 논리적 근거를 위에서 지정한 출력 언어로 작성하세요.
