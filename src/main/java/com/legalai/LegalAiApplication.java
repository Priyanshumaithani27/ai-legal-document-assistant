package com.legalai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the AI Legal Document Assistant.
 *
 * The app lets a user upload a legal document (PDF, DOCX or TXT), then:
 *   1. Extracts the raw text
 *   2. Summarizes it in plain English
 *   3. Extracts key clauses (termination, liability, confidentiality, etc.)
 *   4. Flags a rough risk level
 *   5. Answers free-form follow-up questions about the document
 *
 * AI calls go through AIAnalysisService, which uses a real LLM if an API key
 * is configured (see application.properties), and otherwise falls back to a
 * deterministic rule-based analyzer so the whole app still runs offline/demo-ready.
 */
@SpringBootApplication
public class LegalAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(LegalAiApplication.class, args);
    }
}
