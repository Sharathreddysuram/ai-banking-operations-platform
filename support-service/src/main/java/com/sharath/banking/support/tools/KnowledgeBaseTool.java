package com.sharath.banking.support.tools;

import com.sharath.banking.support.knowledge.KnowledgeCatalog;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class KnowledgeBaseTool {
    private final SimpleVectorStore vectorStore;
    private final List<Document> inMemoryDocs;

    public KnowledgeBaseTool(SimpleVectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.inMemoryDocs = KnowledgeCatalog.documents();
    }

    public List<Document> getAllDocuments() { return inMemoryDocs; }

    public int keywordScore(String question, Document doc) {
        Set<String> qTokens = tokens(question);
        Set<String> dTokens = tokens(doc.getText() + " " + doc.getMetadata().getOrDefault("category", ""));
        qTokens.retainAll(dTokens);
        return qTokens.size();
    }

    public int topKeywordScore(String question) {
        return inMemoryDocs.stream().mapToInt(d -> keywordScore(question, d)).max().orElse(0);
    }

    public List<Document> search(String question) {
        // Keyword-overlap ranking over the in-memory catalogue.
        List<Document> keywordRanked = inMemoryDocs.stream()
                .sorted(Comparator.comparingInt((Document d) -> keywordScore(question, d)).reversed())
                .filter(d -> keywordScore(question, d) > 0)
                .limit(3).toList();
        // Vector store search first per contract, then merge.
        List<Document> vectorResults = List.of();
        try {
            List<Document> found = vectorStore.similaritySearch(
                    SearchRequest.builder().query(question).topK(3).similarityThreshold(0.0).build());
            if (found != null) vectorResults = found;
        } catch (Exception ignored) {
            vectorResults = List.of();
        }
        LinkedHashMap<String, Document> merged = new LinkedHashMap<>();
        for (Document d : keywordRanked) merged.put(d.getText(), d);
        for (Document d : vectorResults) merged.putIfAbsent(d.getText(), d);
        List<Document> result = new ArrayList<>(merged.values());
        return result.stream().limit(3).collect(Collectors.toList());
    }

    private Set<String> tokens(String text) {
        if (text == null) return new HashSet<>();
        return Arrays.stream(text.toLowerCase().split("[^a-z0-9]+"))
                .filter(t -> t.length() > 2)
                .filter(t -> !STOPWORDS.contains(t))
                .collect(Collectors.toSet());
    }
    private static final Set<String> STOPWORDS = Set.of("the","and","for","how","can","what","does","with","your","you","are","this","that","have","from");
}
