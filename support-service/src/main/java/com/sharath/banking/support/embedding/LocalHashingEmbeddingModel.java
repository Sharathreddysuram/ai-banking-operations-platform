package com.sharath.banking.support.embedding;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic 384-dimension hashing embedding model used when no real
 * EmbeddingModel bean exists (fallback mode without Azure OpenAI).
 */
public class LocalHashingEmbeddingModel implements EmbeddingModel {
    public static final int DIMENSIONS = 384;

    @Override
    public float[] embed(String text) {
        float[] vector = new float[DIMENSIONS];
        if (text == null || text.isBlank()) return vector;
        String[] tokens = text.toLowerCase().split("[^a-z0-9]+");
        for (String token : tokens) {
            if (token.isBlank()) continue;
            int idx = Math.floorMod(token.hashCode(), DIMENSIONS);
            vector[idx] += 1.0f;
            int idx2 = Math.floorMod(token.hashCode() * 31 + 7, DIMENSIONS);
            vector[idx2] += 0.5f;
        }
        double norm = 0;
        for (float v : vector) norm += (double) v * v;
        norm = Math.sqrt(norm);
        if (norm > 0) for (int i = 0; i < vector.length; i++) vector[i] = (float) (vector[i] / norm);
        return vector;
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getText());
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        List<String> inputs = request.getInstructions();
        for (int i = 0; i < inputs.size(); i++) {
            embeddings.add(new Embedding(embed(inputs.get(i)), i));
        }
        return new EmbeddingResponse(embeddings, new EmbeddingResponseMetadata());
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }
}
