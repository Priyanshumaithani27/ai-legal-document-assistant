package com.legalai.dto;

import com.legalai.model.LegalDocument;

import java.util.Map;

public class DocumentAnalysisResponse {
    private Long id;
    private String fileName;
    private String documentType;
    private String summary;
    private LegalDocument.RiskLevel riskLevel;
    private String riskNotes;
    private Map<String, String> clauses;

    public DocumentAnalysisResponse(Long id, String fileName, String documentType, String summary,
                                     LegalDocument.RiskLevel riskLevel, String riskNotes,
                                     Map<String, String> clauses) {
        this.id = id;
        this.fileName = fileName;
        this.documentType = documentType;
        this.summary = summary;
        this.riskLevel = riskLevel;
        this.riskNotes = riskNotes;
        this.clauses = clauses;
    }

    public static DocumentAnalysisResponse from(LegalDocument doc) {
        return new DocumentAnalysisResponse(
                doc.getId(),
                doc.getFileName(),
                doc.getDocumentType(),
                doc.getSummary(),
                doc.getRiskLevel(),
                doc.getRiskNotes(),
                doc.getClauses()
        );
    }

    public Long getId() { return id; }
    public String getFileName() { return fileName; }
    public String getDocumentType() { return documentType; }
    public String getSummary() { return summary; }
    public LegalDocument.RiskLevel getRiskLevel() { return riskLevel; }
    public String getRiskNotes() { return riskNotes; }
    public Map<String, String> getClauses() { return clauses; }
}
