package com.example.data.api;

public class Candidate {
    private Content content;

    public Candidate() {}

    public Candidate(Content content) {
        this.content = content;
    }

    public Content getContent() {
        return content;
    }

    public void setContent(Content content) {
        this.content = content;
    }
}
