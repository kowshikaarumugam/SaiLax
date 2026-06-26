package com.example.data.api;

public class InlineData {
    private String mimeType;
    private String data;

    public InlineData() {}

    public InlineData(String mimeType, String data) {
        this.mimeType = mimeType;
        this.data = data;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
