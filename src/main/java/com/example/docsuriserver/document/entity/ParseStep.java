package com.example.docsuriserver.document.entity;

public enum ParseStep {
    QUEUED,
    PREPROCESSING,
    OCR,
    FIELD_EXTRACTION,
    DONE
}
