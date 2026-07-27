-- =====================================================================
-- 마스터 데이터 초기 적재 (01-ERD.md 6절)
-- spring.sql.init.mode=always 로 매 기동 시 실행되므로 재실행에 안전하게 작성한다.
-- =====================================================================

-- disclaimers: 서로게이트 PK라 재기동마다 새로 채운다 (다른 테이블에서 FK로 참조하지 않음)
DELETE FROM disclaimers;
INSERT INTO disclaimers (title, content, display_position, is_active) VALUES
    ('AI 결과 이용 안내', '본 서비스가 제공하는 모든 결과는 AI가 생성한 초안이며 법적 효력이 없습니다.', 'GLOBAL', TRUE),
    ('교차 검증 결과 안내', '본 결과는 AI 초안이며 법적 효력이 없습니다.', 'VALIDATION', TRUE),
    ('HS Code 분류 안내', '최종 분류는 관세당국의 판단에 따릅니다.', 'HS_CODE', TRUE),
    ('정정 요청서 안내', '본 문서는 AI가 생성한 초안이며, 실제 제출 전 반드시 담당자 검토가 필요합니다.', 'CORRECTION', TRUE);

-- trade_terms
INSERT INTO trade_terms (term, full_name, korean_name, description) VALUES
    ('Invoice', 'Commercial Invoice', '상업송장', '수출자가 수입자에게 발행하는 거래 명세서로, 품명·수량·금액이 기재된 서류'),
    ('B/L', 'Bill of Lading', '선하증권', '화물 운송과 인수를 증명하는 서류'),
    ('Packing List', 'Packing List', '포장명세서', '화물의 포장 단위별 수량·중량·부피를 기재한 서류'),
    ('HS Code', 'Harmonized System Code', '품목분류코드', '국제적으로 통일된 상품 분류 코드로, 관세율 결정에 사용'),
    ('FOB', 'Free On Board', '본선인도조건', '수출항 본선에 화물을 적재할 때까지 비용을 매도인이 부담하는 무역조건'),
    ('CIF', 'Cost, Insurance and Freight', '운임보험료포함조건', '수입항까지의 운임·보험료를 매도인이 부담하는 무역조건'),
    ('Shipper', 'Shipper', '송하인', '화물을 운송의뢰하는 수출자'),
    ('Consignee', 'Consignee', '수하인', '화물을 인수하는 수입자'),
    ('POL', 'Port of Loading', '선적항', '화물이 선박에 실리는 항구'),
    ('POD', 'Port of Discharge', '양륙항', '화물이 선박에서 내려지는 항구'),
    ('Gross Weight', 'Gross Weight', '총중량', '포장재를 포함한 화물 전체의 중량'),
    ('Net Weight', 'Net Weight', '순중량', '포장재를 제외한 화물 자체의 중량'),
    ('Shipping Marks', 'Shipping Marks', '화인', '화물 포장 겉면에 표시하는 식별 마크'),
    ('Vessel', 'Vessel', '선박명', '화물을 운송하는 선박의 이름'),
    ('Voyage', 'Voyage Number', '항차', '선박의 운항 회차 번호')
ON CONFLICT (term) DO NOTHING;

-- hs_codes: 데모 품목군을 커버하는 범위로 50건 이상 채운다 (01-ERD.md 6절).
-- 실제 관세율표 전체 데이터는 아니며, HS Code 추천 기능이 후보를 반환할 수 있도록 하는 목적의 시드 데이터다.
INSERT INTO hs_codes (hs_code, korean_name, english_name, description, tariff_rate, import_requirements, updated_at) VALUES
    ('8518.30', '헤드폰과 이어폰', 'Headphones and earphones', '헤드폰과 이어폰(무선 이어폰 포함)', 8, '["전파법에 따른 적합성 평가 여부 확인"]'::jsonb, NOW()),
    ('8518.21', '단일 라우드스피커를 갖춘 것', 'Single loudspeakers, mounted in their enclosures', '인클로저에 장착된 단일 스피커', 8, '[]'::jsonb, NOW()),
    ('9617.00', '보온병과 그 밖의 진공 용기', 'Vacuum flasks and other vacuum vessels', '보온병과 그 밖의 진공 용기(스테인리스 텀블러 포함)', 8, '[]'::jsonb, NOW()),
    ('9405.42', '기타 발광다이오드(LED) 램프', 'Other electric lamps and lighting fittings, LED', 'LED를 광원으로 하는 조명기구', 8, '["전기용품 안전인증(KC) 대상 여부 확인"]'::jsonb, NOW()),
    ('9405.21', '침실용 램프', 'Lamps for bedrooms', '침실·탁상용 램프', 8, '["전기용품 안전인증(KC) 대상 여부 확인"]'::jsonb, NOW()),
    ('3926.90', '기타 플라스틱제품', 'Other articles of plastics', '기타 플라스틱으로 만든 제품', 6.5, '[]'::jsonb, NOW()),
    ('8471.30', '휴대용 자동자료처리기계(노트북 등)', 'Portable automatic data processing machines', '노트북 컴퓨터 등 휴대용 컴퓨터', 0, '["전파법에 따른 적합성 평가 여부 확인"]'::jsonb, NOW()),
    ('8471.41', '기타 자동자료처리기계(데스크톱 등)', 'Other automatic data processing machines', '데스크톱 컴퓨터 등', 0, '[]'::jsonb, NOW()),
    ('8517.13', '스마트폰', 'Smartphones', '스마트폰', 0, '["전파법에 따른 적합성 평가 여부 확인"]'::jsonb, NOW()),
    ('8517.62', '데이터 송수신용 기기(라우터 등)', 'Machines for reception/conversion/transmission of data', '유무선 공유기 등 네트워크 장비', 0, '["전파법에 따른 적합성 평가 여부 확인"]'::jsonb, NOW()),
    ('8506.50', '리튬 전지', 'Lithium batteries', '리튬 1차전지', 0, '["위험물 운송 규정 확인"]'::jsonb, NOW()),
    ('8507.60', '리튬이온 축전지', 'Lithium-ion batteries', '리튬이온 2차전지(보조배터리 포함)', 0, '["위험물 운송 규정 확인", "전기용품 안전인증(KC) 대상 여부 확인"]'::jsonb, NOW()),
    ('6109.10', '면제 티셔츠', 'T-shirts, singlets, cotton', '면으로 만든 티셔츠', 13, '["원산지 표시 확인"]'::jsonb, NOW()),
    ('6110.20', '면제 스웨터·풀오버', 'Sweaters, pullovers, cotton', '면 편물제 스웨터류', 13, '["원산지 표시 확인"]'::jsonb, NOW()),
    ('6203.42', '면제 남성용 바지', 'Men''s trousers, cotton', '면으로 만든 남성용 바지', 13, '["원산지 표시 확인"]'::jsonb, NOW()),
    ('6204.62', '면제 여성용 바지', 'Women''s trousers, cotton', '면으로 만든 여성용 바지', 13, '["원산지 표시 확인"]'::jsonb, NOW()),
    ('6402.99', '기타 신발류(고무·플라스틱제)', 'Other footwear, rubber or plastics', '고무 또는 플라스틱 밑창의 기타 신발', 13, '[]'::jsonb, NOW()),
    ('4202.22', '핸드백(외면 플라스틱시트·방직용섬유)', 'Handbags, outer surface of plastic sheeting or textile', '플라스틱·섬유 외피 핸드백', 8, '[]'::jsonb, NOW()),
    ('9503.00', '완구류', 'Toys', '완구(세발자전거·인형·모형 등 포함)', 0, '["어린이제품 안전인증(KC) 대상 여부 확인"]'::jsonb, NOW()),
    ('9506.62', '축구공 등 인플레이터블 볼', 'Inflatable balls', '공기주입식 볼(축구공 등)', 0, '[]'::jsonb, NOW()),
    ('4901.99', '기타 인쇄된 서적', 'Other printed books', '기타 인쇄된 서적·책자', 0, '[]'::jsonb, NOW()),
    ('4820.10', '노트·메모지 등', 'Registers, notebooks', '공책·메모장 등 사무용지 제품', 0, '[]'::jsonb, NOW()),
    ('9018.90', '기타 의료용 기기', 'Other instruments used in medical sciences', '기타 의료·수술용 기기', 0, '["의료기기 허가 대상 여부 확인"]'::jsonb, NOW()),
    ('3304.99', '기타 화장품(기초화장용)', 'Other beauty or make-up preparations', '기초화장용 제품류', 6.5, '["화장품법에 따른 수입관리 대상 여부 확인"]'::jsonb, NOW()),
    ('3401.11', '화장 비누', 'Soap for toilet use', '세면·화장용 비누', 6.5, '[]'::jsonb, NOW()),
    ('2101.11', '커피 추출물·엑기스', 'Extracts, essences of coffee', '인스턴트 커피 등 커피 추출물', 8, '["식품 등 수입신고 대상 확인"]'::jsonb, NOW()),
    ('1806.90', '기타 코코아 함유 조제식품', 'Other food preparations containing cocoa', '초콜릿 가공식품', 8, '["식품 등 수입신고 대상 확인"]'::jsonb, NOW()),
    ('2202.99', '기타 비알코올 음료', 'Other non-alcoholic beverages', '기타 비알코올 음료', 8, '["식품 등 수입신고 대상 확인"]'::jsonb, NOW()),
    ('0304.71', '냉동 대구', 'Frozen cod', '냉동 대구(필렛 제외)', 0, '["수산물 검역 대상 확인"]'::jsonb, NOW()),
    ('0303.54', '냉동 고등어', 'Frozen mackerel', '냉동 고등어', 10, '["수산물 검역 대상 확인"]'::jsonb, NOW()),
    ('0808.10', '사과(신선)', 'Apples, fresh', '신선한 사과', 45, '["식물 검역 대상 확인"]'::jsonb, NOW()),
    ('0901.21', '볶은 커피(카페인 함유)', 'Coffee, roasted, not decaffeinated', '볶은 원두커피', 8, '["식품 등 수입신고 대상 확인"]'::jsonb, NOW()),
    ('8703.23', '승용자동차(1500~3000cc)', 'Motor cars, 1500-3000cc', '배기량 1500~3000cc 승용차', 8, '["자동차 안전기준 인증 대상 확인"]'::jsonb, NOW()),
    ('8711.20', '이륜자동차(50~250cc)', 'Motorcycles, 50-250cc', '배기량 50~250cc 이륜차', 8, '["자동차 안전기준 인증 대상 확인"]'::jsonb, NOW()),
    ('8712.00', '자전거', 'Bicycles', '모터 없는 자전거', 0, '["어린이제품 안전인증(KC) 대상 여부 확인"]'::jsonb, NOW()),
    ('9401.61', '목제 프레임 좌석(속을 채운 것)', 'Upholstered seats with wooden frames', '목재 프레임 소파·의자', 0, '[]'::jsonb, NOW()),
    ('9403.30', '목제 사무용 가구', 'Wooden furniture for offices', '목재 사무용 가구', 0, '[]'::jsonb, NOW()),
    ('7013.49', '기타 유리제 식탁용품', 'Other glassware for table use', '유리제 식기·용기', 8, '[]'::jsonb, NOW()),
    ('6911.10', '도자제 식탁용품', 'Tableware of porcelain or china', '도자기 식기류', 8, '[]'::jsonb, NOW()),
    ('8414.51', '탁상용·바닥용 선풍기', 'Table, floor fans', '가정용 선풍기', 8, '["전기용품 안전인증(KC) 대상 여부 확인"]'::jsonb, NOW()),
    ('8415.10', '창문형·벽걸이형 에어컨', 'Window or wall air conditioning machines', '창문형·벽걸이형 에어컨', 8, '["전기용품 안전인증(KC) 대상 여부 확인"]'::jsonb, NOW()),
    ('8450.11', '전자동 세탁기(6kg 이하)', 'Fully-automatic washing machines, <=6kg', '가정용 전자동 세탁기', 8, '["전기용품 안전인증(KC) 대상 여부 확인"]'::jsonb, NOW()),
    ('8508.11', '진공청소기(1500W 이하)', 'Vacuum cleaners, power <=1500W', '가정용 진공청소기', 8, '["전기용품 안전인증(KC) 대상 여부 확인"]'::jsonb, NOW()),
    ('9002.11', '카메라용 대물렌즈', 'Objective lenses for cameras', '카메라 렌즈', 8, '[]'::jsonb, NOW()),
    ('9006.53', '기타 사진기(필름폭 35mm)', 'Other cameras, 35mm film', '필름 카메라', 8, '[]'::jsonb, NOW()),
    ('8544.42', '커넥터 부착 전선(USB 케이블 등)', 'Electric conductors, fitted with connectors', 'USB 케이블 등 커넥터 부착 전선', 0, '[]'::jsonb, NOW()),
    ('8536.69', '플러그·소켓', 'Plugs and sockets', '전기용 플러그·콘센트', 8, '["전기용품 안전인증(KC) 대상 여부 확인"]'::jsonb, NOW()),
    ('3923.21', '폴리에틸렌제 포대·봉지', 'Sacks and bags of polymers of ethylene', '비닐 포장재', 6.5, '[]'::jsonb, NOW()),
    ('4819.10', '골판지 상자', 'Cartons, boxes of corrugated paper', '골판지 포장 박스', 0, '[]'::jsonb, NOW()),
    ('4202.92', '기타 케이스류(외면 플라스틱시트·방직용섬유)', 'Other containers, outer surface of plastic sheeting or textile', '파우치·케이스류', 8, '[]'::jsonb, NOW())
ON CONFLICT (hs_code) DO NOTHING;
