package com.example.docsuriserver.entity;

public enum ParseStep {
    QUEUED,
    PREPROCESSING,
    OCR,
    FIELD_EXTRACTION,
    DONE
}
