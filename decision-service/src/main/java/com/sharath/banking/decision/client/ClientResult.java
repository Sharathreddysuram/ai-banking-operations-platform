package com.sharath.banking.decision.client;
public record ClientResult(String summary, Double riskScore, boolean available) {
    public static ClientResult unavailable(String label, String err) {
        return new ClientResult(label + " unavailable: " + err, null, false);
    }
    public double riskOrDefault(double def) { return riskScore != null ? riskScore : def; }
}
