package com.legalai;

import com.legalai.service.ClauseExtractionService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClauseExtractionServiceTest {

    private final ClauseExtractionService service = new ClauseExtractionService();

    @Test
    void findsTerminationAndConfidentialityClauses() {
        String sample =
                "This Agreement may be terminated by either party with 30 days written notice.\n\n" +
                "Each party agrees to keep all confidential and proprietary information private.\n\n" +
                "This Agreement is subject to the governing law and jurisdiction of the State of Delaware.";

        Map<String, String> clauses = service.extractClauses(sample);

        assertTrue(clauses.containsKey("Termination"));
        assertTrue(clauses.containsKey("Confidentiality"));
        assertTrue(clauses.containsKey("Governing Law"));
    }
}
