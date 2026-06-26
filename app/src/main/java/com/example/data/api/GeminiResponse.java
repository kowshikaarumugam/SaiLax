package com.example.data.api;

import java.util.List;

public class GeminiResponse {
    private List<Candidate> candidates;

    public GeminiResponse() {}

    public GeminiResponse(List<Candidate> candidates) {
        this.candidates = candidates;
    }

    public List<Candidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<Candidate> candidates) {
        this.candidates = candidates;
    }
}
