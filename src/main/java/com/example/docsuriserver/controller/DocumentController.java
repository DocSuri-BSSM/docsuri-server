package com.example.docsuriserver.controller;

import com.example.docsuriserver.common.ApiResponse;
import com.example.docsuriserver.dto.DocumentUploadResponse;
import com.example.docsuriserver.service.DocumentUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentUploadService documentUploadService;

    public DocumentController(DocumentUploadService documentUploadService) {
        this.documentUploadService = documentUploadService;
    }

    /** POST /documents — documentUpload */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<DocumentUploadResponse>> documentUpload(
            @RequestPart(value = "invoice_file", required = false) MultipartFile invoiceFile,
            @RequestPart(value = "bill_of_lading_file", required = false) MultipartFile billOfLadingFile,
            @RequestPart(value = "packing_list_file", required = false) MultipartFile packingListFile) {

        DocumentUploadResponse data =
                documentUploadService.upload(invoiceFile, billOfLadingFile, packingListFile);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("문서 업로드 성공", data));
    }
}
