package com.example.data.api;

public class GenerationConfig {
    private String responseMimeType;
    private Float temperature;

    public GenerationConfig() {}

    public GenerationConfig(String responseMimeType, Float temperature) {
        this.responseMimeType = responseMimeType;
        this.temperature = temperature;
    }

    public String getResponseMimeType() {
        return responseMimeType;
    }

    public void setResponseMimeType(String responseMimeType) {
        this.responseMimeType = responseMimeType;
    }

    public Float getTemperature() {
        return temperature;
    }

    public void setTemperature(Float temperature) {
        this.temperature = temperature;
    }
}
