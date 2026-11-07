package com.sharath.banking.support.config;

import com.sharath.banking.support.embedding.LocalHashingEmbeddingModel;
import com.sharath.banking.support.knowledge.KnowledgeCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {
    private static final Logger log = LoggerFactory.getLogger(RagConfig.class);

    @Bean
    public SimpleVectorStore vectorStore(ObjectProvider<EmbeddingModel> embeddingModelProvider) {
        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable(LocalHashingEmbeddingModel::new);
        log.info("Building SimpleVectorStore with embedding model {}", embeddingModel.getClass().getSimpleName());
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean
    public ApplicationRunner knowledgeLoader(SimpleVectorStore vectorStore) {
        return args -> {
            vectorStore.add(KnowledgeCatalog.documents());
            log.info("Loaded {} knowledge documents into vector store", KnowledgeCatalog.all().size());
        };
    }
}
