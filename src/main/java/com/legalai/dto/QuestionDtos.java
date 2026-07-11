package com.legalai.dto;

import jakarta.validation.constraints.NotBlank;

public class QuestionDtos {

    public static class QuestionRequest {
        @NotBlank(message = "question must not be empty")
        private String question;

        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
    }

    public static class QuestionResponse {
        private final String question;
        private final String answer;

        public QuestionResponse(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }

        public String getQuestion() { return question; }
        public String getAnswer() { return answer; }
    }
}
