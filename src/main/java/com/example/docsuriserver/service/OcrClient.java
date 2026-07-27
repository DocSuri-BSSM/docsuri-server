package com.example.docsuriserver.service;

import com.example.docsuriserver.entity.Document;
import com.example.docsuriserver.entity.ExtractedField;

import java.util.List;

/**
 * 실제 OCR 엔진(Google Vision / Naver CLOVA OCR / Upstage 등) 연동 지점.
 * 지금은 MockOcrClient 로 동작시키고, 프론트 연동이 끝난 뒤 구현체만 교체한다.
 */
public interface OcrClient {

    List<ExtractedField> extract(Document document, String ocrLanguage, List<String> extractFields);
}
