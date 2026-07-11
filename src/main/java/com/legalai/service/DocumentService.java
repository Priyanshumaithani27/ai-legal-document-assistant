package com.legalai.service;

import com.legalai.model.LegalDocument;
import com.legalai.repository.LegalDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class DocumentService {

    private final TextExtractionService textExtractionService;
    private final ClauseExtractionService clauseExtractionService;
    private final AIAnalysisService aiAnalysisService;
    private final LegalDocumentRepository repository;

    public DocumentService(TextExtractionService textExtractionService,
                            ClauseExtractionService clauseExtractionService,
                            AIAnalysisService aiAnalysisService,
                            LegalDocumentRepository repository) {
        this.textExtractionService = textExtractionService;
        this.clauseExtractionService = clauseExtractionService;
        this.aiAnalysisService = aiAnalysisService;
        this.repository = repository;
    }

    public LegalDocument processUpload(MultipartFile file) throws IOException {
        String text = textExtractionService.extractText(file);
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("No extractable text was found in the uploaded file.");
        }

        Map<String, String> clauses = clauseExtractionService.extractClauses(text);
        String documentType = aiAnalysisService.classifyDocumentType(text);
        String summary = aiAnalysisService.summarize(text);
        AIAnalysisService.RiskAssessment risk = aiAnalysisService.assessRisk(text, clauses);

        LegalDocument doc = new LegalDocument();
        doc.setFileName(file.getOriginalFilename());
        doc.setDocumentType(documentType);
        doc.setExtractedText(text);
        doc.setSummary(summary);
        doc.setClauses(clauses);
        doc.setRiskLevel(risk.level);
        doc.setRiskNotes(risk.notes);

        return repository.save(doc);
    }

    public LegalDocument getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Document not found: " + id));
    }

    public java.util.List<LegalDocument> getAll() {
        return repository.findAll();
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public String answerQuestion(Long id, String question) {
        LegalDocument doc = getById(id);
        return aiAnalysisService.answerQuestion(doc.getExtractedText(), doc.getSummary(), question);
    }
}
