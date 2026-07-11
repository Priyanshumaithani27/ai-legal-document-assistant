package com.legalai.controller;

import com.legalai.dto.DocumentAnalysisResponse;
import com.legalai.dto.QuestionDtos.QuestionRequest;
import com.legalai.dto.QuestionDtos.QuestionResponse;
import com.legalai.model.LegalDocument;
import com.legalai.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<DocumentAnalysisResponse> upload(@RequestParam("file") MultipartFile file) throws IOException {
        LegalDocument saved = documentService.processUpload(file);
        return ResponseEntity.ok(DocumentAnalysisResponse.from(saved));
    }

    @GetMapping
    public ResponseEntity<List<DocumentAnalysisResponse>> listAll() {
        List<DocumentAnalysisResponse> docs = documentService.getAll().stream()
                .map(DocumentAnalysisResponse::from)
                .toList();
        return ResponseEntity.ok(docs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentAnalysisResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(DocumentAnalysisResponse.from(documentService.getById(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/ask")
    public ResponseEntity<QuestionResponse> ask(@PathVariable Long id, @Valid @RequestBody QuestionRequest request) {
        String answer = documentService.answerQuestion(id, request.getQuestion());
        return ResponseEntity.ok(new QuestionResponse(request.getQuestion(), answer));
    }
}
