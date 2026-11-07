package com.sharath.banking.decision.client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
@Component
public class LoanClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;
    public LoanClient(RestTemplate restTemplate, @Value("${services.loan-url}") String baseUrl) {
        this.restTemplate = restTemplate; this.baseUrl = baseUrl;
    }
    public ClientResult getLoanSummary(String customerId) {
        return fetch(baseUrl + "/api/loan/applications?customerId=" + customerId, "Loan");
    }

    protected ClientResult fetch(String url, String label) {
        try {
            ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                String body = resp.getBody();
                String summary = label + ": " + (body.length() > 500 ? body.substring(0, 500) : body);
                return new ClientResult(summary, extractRisk(body), true);
            }
            return new ClientResult(label + " unavailable: HTTP " + resp.getStatusCode().value(), null, false);
        } catch (Exception e) {
            return new ClientResult(label + " unavailable: " + e.getMessage(), null, false);
        }
    }
    protected Double extractRisk(String body) {
        if (body == null) return null;
        Double parsed = parseNumberAfter(body, "\"riskScore\"");
        if (parsed != null) return parsed;
        parsed = parseNumberAfter(body, "\"overallRiskScore\"");
        if (parsed != null) return parsed;
        String lower = body.toLowerCase();
        if (lower.contains("critical") || lower.contains("\"severity\":\"high\"")) return 85.0;
        if (lower.contains("verified")) return 10.0;
        if (lower.contains("approve") && !lower.contains("manual")) return 20.0;
        if (lower.contains("manual_review")) return 60.0;
        if (lower.contains("reject")) return 80.0;
        return null;
    }


    protected Double parseNumberAfter(String body, String key) {
        int idx = body.indexOf(key);
        if (idx < 0) return null;
        int colon = body.indexOf(':', idx + key.length());
        if (colon < 0) return null;
        int i = colon + 1;
        while (i < body.length() && (body.charAt(i) == ' ' || body.charAt(i) == '\t')) i++;
        int start = i;
        while (i < body.length() && (Character.isDigit(body.charAt(i)) || body.charAt(i) == '.')) i++;
        if (i == start) return null;
        try { return Double.parseDouble(body.substring(start, i)); } catch (Exception e) { return null; }
    }
}
