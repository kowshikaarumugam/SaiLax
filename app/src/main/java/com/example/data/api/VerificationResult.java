package com.example.data.api;

public class VerificationResult {
    private boolean verified;
    private double confidence;
    private String reason;

    public VerificationResult() {}

    public VerificationResult(boolean verified, double confidence, String reason) {
        this.verified = verified;
        this.confidence = confidence;
        this.reason = reason;
    }

    public boolean isVerified() {
        return verified;
    }

    public boolean getVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
