package com.example.data.api;

public class Part {
    private String text;
    private InlineData inlineData;

    public Part() {}

    public Part(String text) {
        this.text = text;
        this.inlineData = null;
    }

    public Part(InlineData inlineData) {
        this.text = null;
        this.inlineData = inlineData;
    }

    public Part(String text, InlineData inlineData) {
        this.text = text;
        this.inlineData = inlineData;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public InlineData getInlineData() {
        return inlineData;
    }

    public void setInlineData(InlineData inlineData) {
        this.inlineData = inlineData;
    }
}
