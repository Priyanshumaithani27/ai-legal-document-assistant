package com.legalai.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "legal_documents")
public class LegalDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    private String documentType; // e.g. NDA, LEASE, EMPLOYMENT, CONTRACT, UNKNOWN

    @Lob
    @Column(length = 500000)
    private String extractedText;

    @Lob
    @Column(length = 50000)
    private String summary;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    @Lob
    @Column(length = 50000)
    private String riskNotes;

    @ElementCollection
    @CollectionTable(name = "document_clauses", joinColumns = @JoinColumn(name = "document_id"))
    @MapKeyColumn(name = "clause_type")
    @Column(name = "clause_text", length = 4000)
    @Lob
    private Map<String, String> clauses = new HashMap<>();

    private LocalDateTime uploadedAt = LocalDateTime.now();

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, UNKNOWN
    }

    // ---- getters / setters (written out explicitly, no Lombok needed) ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public String getRiskNotes() { return riskNotes; }
    public void setRiskNotes(String riskNotes) { this.riskNotes = riskNotes; }

    public Map<String, String> getClauses() { return clauses; }
    public void setClauses(Map<String, String> clauses) { this.clauses = clauses; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
