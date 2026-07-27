package com.example.docsuriserver.document.service;

import com.example.docsuriserver.document.entity.ExtractedField;

import java.util.List;

public interface OcrClient {

    List<ExtractedField> extract(OcrRequest request);
}
