package com.legalai.service;

import com.legalai.config.AppConfig.LlmProperties;
import com.legalai.model.LegalDocument;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Produces summary / document type / risk assessment / Q&A answers.
 *
 * If llm.api-key is set (application.properties or LLM_API_KEY env var),
 * calls a real LLM (OpenAI-compatible chat completions endpoint).
 * Otherwise, uses a deterministic offline analyzer so the whole
 * application still runs and demos correctly with zero external setup.
 */
@Service
public class AIAnalysisService {

    private final RestTemplate restTemplate;
    private final LlmProperties llmProperties;
    private final ClauseExtractionService clauseExtractionService;

    public AIAnalysisService(RestTemplate restTemplate,
                              LlmProperties llmProperties,
                              ClauseExtractionService clauseExtractionService) {
        this.restTemplate = restTemplate;
        this.llmProperties = llmProperties;
        this.clauseExtractionService = clauseExtractionService;
    }

    public boolean isLlmConfigured() {
        return llmProperties.getApiKey() != null && !llmProperties.getApiKey().isBlank();
    }

    // ---------------------------------------------------------------- summary

    public String summarize(String text) {
        if (isLlmConfigured()) {
            String prompt = "Summarize the following legal document in 5-8 plain-English bullet points "
                    + "a non-lawyer can understand. Focus on obligations, rights, money, and dates.\n\n"
                    + truncate(text, 12000);
            String result = callLlm(prompt);
            if (result != null) return result;
        }
        return offlineSummary(text);
    }

    private String offlineSummary(String text) {
        String[] sentences = text.split("(?<=[.!?])\\s+");
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String s : sentences) {
            String clean = s.replaceAll("\\s+", " ").trim();
            if (clean.length() < 40) continue; // skip headers / short fragments
            sb.append("- ").append(clean).append("\n");
            count++;
            if (count >= 6) break;
        }
        if (count == 0) {
            sb.append("- Document too short or unstructured to summarize automatically.\n");
        }
        sb.append("\n(Offline mode: keyword-based summary. Configure llm.api-key for an AI-generated summary.)");
        return sb.toString();
    }

    // ------------------------------------------------------------ doc type

    public String classifyDocumentType(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("non-disclosure") || lower.contains("confidentiality agreement")) return "NDA";
        if (lower.contains("lease") || lower.contains("landlord") || lower.contains("tenant")) return "LEASE";
        if (lower.contains("employment agreement") || lower.contains("employee") && lower.contains("employer")) return "EMPLOYMENT";
        if (lower.contains("terms of service") || lower.contains("terms and conditions")) return "TERMS_OF_SERVICE";
        if (lower.contains("master service agreement") || lower.contains("statement of work")) return "SERVICE_AGREEMENT";
        if (lower.contains("agreement") || lower.contains("contract")) return "CONTRACT";
        return "UNKNOWN";
    }

    // ---------------------------------------------------------------- risk

    public static class RiskAssessment {
        public LegalDocument.RiskLevel level;
        public String notes;
        RiskAssessment(LegalDocument.RiskLevel level, String notes) {
            this.level = level; this.notes = notes;
        }
    }

    public RiskAssessment assessRisk(String text, Map<String, String> clauses) {
        if (isLlmConfigured()) {
            String prompt = "You are a contract risk reviewer. Based on the clauses below, respond with exactly "
                    + "one line 'RISK: LOW' or 'RISK: MEDIUM' or 'RISK: HIGH', then a blank line, then 3-5 bullet "
                    + "points explaining concerning clauses (unlimited liability, auto-renewal, one-sided "
                    + "termination, broad non-competes, missing caps, etc). If nothing stands out, say so.\n\n"
                    + clauses;
            String result = callLlm(prompt);
            if (result != null) {
                return parseLlmRisk(result);
            }
        }
        return offlineRiskHeuristic(text, clauses);
    }

    private RiskAssessment parseLlmRisk(String llmOutput) {
        LegalDocument.RiskLevel level = LegalDocument.RiskLevel.UNKNOWN;
        String upper = llmOutput.toUpperCase();
        if (upper.contains("RISK: HIGH")) level = LegalDocument.RiskLevel.HIGH;
        else if (upper.contains("RISK: MEDIUM")) level = LegalDocument.RiskLevel.MEDIUM;
        else if (upper.contains("RISK: LOW")) level = LegalDocument.RiskLevel.LOW;
        return new RiskAssessment(level, llmOutput);
    }

    private RiskAssessment offlineRiskHeuristic(String text, Map<String, String> clauses) {
        List<String> flags = new ArrayList<>();
        String lower = text.toLowerCase();

        if (lower.contains("unlimited liability") || (lower.contains("liability") && !lower.contains("limitation of liability"))) {
            flags.add("Liability language present without an obvious cap - review the Liability clause.");
        }
        if (lower.contains("automatically renew") || lower.contains("auto-renew")) {
            flags.add("Contract appears to auto-renew - check the notice period required to opt out.");
        }
        if (lower.contains("sole discretion")) {
            flags.add("One party can act at their 'sole discretion' in at least one clause - potentially one-sided.");
        }
        if (clauses.containsKey("Non-Compete")) {
            flags.add("A non-compete / restraint-of-trade clause was found - confirm scope and duration are reasonable.");
        }
        if (!clauses.containsKey("Termination")) {
            flags.add("No clear termination clause detected - unclear how either party can exit the agreement.");
        }
        if (!clauses.containsKey("Governing Law")) {
            flags.add("No governing law / jurisdiction clause detected.");
        }

        LegalDocument.RiskLevel level;
        if (flags.size() >= 4) level = LegalDocument.RiskLevel.HIGH;
        else if (flags.size() >= 2) level = LegalDocument.RiskLevel.MEDIUM;
        else if (flags.isEmpty()) level = LegalDocument.RiskLevel.LOW;
        else level = LegalDocument.RiskLevel.MEDIUM;

        String notes = flags.isEmpty()
                ? "No major red flags detected by the offline heuristic scan."
                : String.join("\n", flags.stream().map(f -> "- " + f).toList());
        notes += "\n\n(Offline mode: rule-based scan. Configure llm.api-key for AI-based risk review.)";

        return new RiskAssessment(level, notes);
    }

    // ------------------------------------------------------------------ Q&A

    public String answerQuestion(String documentText, String summary, String question) {
        if (isLlmConfigured()) {
            String prompt = "You are a legal document assistant. Answer the user's question using ONLY the "
                    + "document text below. If the answer isn't in the document, say so clearly. Do not give "
                    + "formal legal advice - add a short reminder to consult a licensed attorney for decisions.\n\n"
                    + "DOCUMENT:\n" + truncate(documentText, 12000)
                    + "\n\nQUESTION: " + question;
            String result = callLlm(prompt);
            if (result != null) return result;
        }
        return offlineAnswer(documentText, question);
    }

    private String offlineAnswer(String documentText, String question) {
        String[] keywords = question.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .split("\\s+");
        String[] sentences = documentText.split("(?<=[.!?])\\s+");

        List<String> matches = new ArrayList<>();
        for (String sentence : sentences) {
            String lower = sentence.toLowerCase();
            for (String kw : keywords) {
                if (kw.length() > 3 && lower.contains(kw)) {
                    matches.add(sentence.trim());
                    break;
                }
            }
            if (matches.size() >= 3) break;
        }

        if (matches.isEmpty()) {
            return "I couldn't find text in the document matching your question. Try rephrasing, or configure "
                    + "llm.api-key for full AI-powered Q&A. This tool does not provide legal advice - consult a "
                    + "licensed attorney for decisions.";
        }
        return "Based on matching text in the document:\n\n"
                + String.join("\n\n", matches.stream().map(m -> "\"" + m + "\"").toList())
                + "\n\n(Offline mode: keyword match, not true comprehension. Configure llm.api-key for AI-based "
                + "answers.) This is not legal advice - consult a licensed attorney.";
    }

    // -------------------------------------------------------------- LLM call

    /**
     * Calls an OpenAI-compatible /chat/completions endpoint.
     * Returns null on any failure so callers can fall back gracefully.
     */
    private String callLlm(String userPrompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(llmProperties.getApiKey());

            Map<String, Object> body = new HashMap<>();
            body.put("model", llmProperties.getModel());
            body.put("messages", List.of(
                    Map.of("role", "system", "content",
                            "You are a careful legal-document analysis assistant. You are not a lawyer and never "
                                    + "give definitive legal advice; you explain plainly and suggest consulting an attorney "
                                    + "for anything consequential."),
                    Map.of("role", "user", "content", userPrompt)
            ));
            body.put("temperature", 0.2);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(llmProperties.getApiUrl(), request, Map.class);
            if (response == null) return null;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) return null;

            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            // Network / auth / quota failure - fall back to offline mode rather than 500ing.
            return null;
        }
    }

    private String truncate(String text, int maxChars) {
        return text.length() <= maxChars ? text : text.substring(0, maxChars);
    }
}
