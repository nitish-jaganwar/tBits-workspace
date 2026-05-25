package com.tbits.ai.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class RAGService {

	private static final Logger logger = Logger.getLogger(RAGService.class.getName());

	private DocumentAssistant assistant;

	@Value("${openai.api.key}")
	private String openAiApiKey;

	@Value("${milvus.url}")
	private String milvusUrl;

	@Value("${milvus.token}")
	private String milvusToken;

	@Value("${milvus.collection}")
	private String collectionName;

	@Value("${embedding.dimension}")
	private Integer embeddingDimension;

	interface DocumentAssistant {

		@SystemMessage({ "You are an elite AI assistant.", "Answer ONLY from provided context.",
				"If answer not found say:", "'Sorry, I don't have this information in my current database.'" })

		String chat(String userMessage);
	}

	@PostConstruct 
	public void init() {

		try {

			logger.info("🚀 Initializing AI Engine...");

			ChatLanguageModel chatModel = OpenAiChatModel.builder().apiKey(openAiApiKey).modelName("gpt-4o")
					.temperature(0.0).build();

			EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder().apiKey(openAiApiKey)
					.modelName("text-embedding-3-large").build();

			EmbeddingStore<TextSegment> milvusStore = MilvusEmbeddingStore.builder().uri(milvusUrl).token(milvusToken)
					.collectionName(collectionName).dimension(3072).build();

			ContentRetriever retriever = EmbeddingStoreContentRetriever.builder().embeddingStore(milvusStore)
					.embeddingModel(embeddingModel).maxResults(5).minScore(0.4).build();

			assistant = AiServices.builder(DocumentAssistant.class).chatLanguageModel(chatModel)
					.contentRetriever(retriever).chatMemory(MessageWindowChatMemory.withMaxMessages(10)).build();

			logger.info("✅ AI Engine Ready");

		} catch (Exception e) {

			logger.severe("❌ Initialization Failed: " + e.getMessage());
		}
	}

	public String getAnswer(String query) {

		if (assistant == null) {
			throw new RuntimeException("AI Engine not initialized");
		}

		return assistant.chat(query);
	}
}
