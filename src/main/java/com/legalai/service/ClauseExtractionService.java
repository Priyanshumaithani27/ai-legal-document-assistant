package com.legalai.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the sentence/paragraph windows in a contract that most likely
 * correspond to well-known legal clause types. This is a lightweight,
 * fully offline keyword+regex approach used either standalone, or as
 * candidate context that gets handed to the LLM for a cleaner rewrite.
 */
@Service
public class ClauseExtractionService {

    private static final Map<String, String[]> CLAUSE_KEYWORDS = new LinkedHashMap<>();
    static {
        CLAUSE_KEYWORDS.put("Termination", new String[]{"terminat", "end this agreement", "notice of cancellation"});
        CLAUSE_KEYWORDS.put("Confidentiality", new String[]{"confidential", "non-disclosure", "proprietary information"});
        CLAUSE_KEYWORDS.put("Indemnification", new String[]{"indemnif", "hold harmless"});
        CLAUSE_KEYWORDS.put("Liability", new String[]{"limitation of liability", "liable", "consequential damages"});
        CLAUSE_KEYWORDS.put("Governing Law", new String[]{"governing law", "jurisdiction", "venue"});
        CLAUSE_KEYWORDS.put("Payment Terms", new String[]{"payment", "invoice", "fees shall", "compensation"});
        CLAUSE_KEYWORDS.put("Intellectual Property", new String[]{"intellectual property", "copyright", "trademark", "work product"});
        CLAUSE_KEYWORDS.put("Non-Compete", new String[]{"non-compete", "non compete", "restraint of trade"});
        CLAUSE_KEYWORDS.put("Dispute Resolution", new String[]{"arbitration", "mediation", "dispute resolution"});
        CLAUSE_KEYWORDS.put("Force Majeure", new String[]{"force majeure", "act of god"});
    }

    /**
     * Splits text into paragraphs and returns, for each known clause type,
     * the first paragraph whose lowercase text contains one of its keywords.
     */
    public Map<String, String> extractClauses(String text) {
        Map<String, String> found = new LinkedHashMap<>();
        if (text == null || text.isBlank()) {
            return found;
        }

        String[] paragraphs = splitIntoParagraphs(text);

        for (Map.Entry<String, String[]> entry : CLAUSE_KEYWORDS.entrySet()) {
            String clauseType = entry.getKey();
            String[] keywords = entry.getValue();

            for (String paragraph : paragraphs) {
                String lower = paragraph.toLowerCase();
                for (String keyword : keywords) {
                    if (lower.contains(keyword)) {
                        found.put(clauseType, trim(paragraph, 600));
                        break;
                    }
                }
                if (found.containsKey(clauseType)) break;
            }
        }
        return found;
    }

    private String[] splitIntoParagraphs(String text) {
        // Split on blank lines first; if the document has no blank lines
        // (common with PDF extraction), fall back to splitting on sentences.
        String[] byBlankLine = text.split("\\r?\\n\\s*\\r?\\n");
        if (byBlankLine.length > 3) {
            return byBlankLine;
        }
        return text.split("(?<=[.!?])\\s+(?=[A-Z0-9])");
    }

    private String trim(String s, int maxLen) {
        String cleaned = s.replaceAll("\\s+", " ").trim();
        return cleaned.length() > maxLen ? cleaned.substring(0, maxLen) + "..." : cleaned;
    }
}
