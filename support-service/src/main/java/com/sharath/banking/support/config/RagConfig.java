package com.sharath.banking.support.config;

import com.sharath.banking.support.embedding.LocalHashingEmbeddingModel;
import com.sharath.banking.support.knowledge.KnowledgeCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {
    private static final Logger log = LoggerFactory.getLogger(RagConfig.class);

    /**
     * In fallback mode the platform must run with no Azure OpenAI access at all,
     * so the vector store is built on the local hashing embedding model even when
     * an Azure embedding model bean exists. In azure mode the configured cloud
     * embedding model is used (with the local model as a last resort).
     */
    @Bean
    public SimpleVectorStore vectorStore(ObjectProvider<EmbeddingModel> embeddingModelProvider,
                                         @Value("${ai.mode:fallback}") String aiMode) {
        EmbeddingModel embeddingModel;
        if ("azure".equalsIgnoreCase(aiMode)) {
            embeddingModel = embeddingModelProvider.getIfAvailable(LocalHashingEmbeddingModel::new);
        } else {
            embeddingModel = new LocalHashingEmbeddingModel();
        }
        log.info("Building SimpleVectorStore with embedding model {}", embeddingModel.getClass().getSimpleName());
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * Loading the knowledge base is best-effort: a RAG failure (for example a
     * rejected embedding call) must never take the whole service down.
     */
    @Bean
    public ApplicationRunner knowledgeLoader(SimpleVectorStore vectorStore) {
        return args -> {
            try {
                vectorStore.add(KnowledgeCatalog.documents());
                log.info("Loaded {} knowledge documents into vector store", KnowledgeCatalog.all().size());
            } catch (Exception e) {
                log.error("Knowledge base load failed; support service continues without RAG index", e);
            }
        };
    }
}
