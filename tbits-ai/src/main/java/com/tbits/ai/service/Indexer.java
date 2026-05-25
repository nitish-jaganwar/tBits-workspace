package com.tbits.ai.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;

public class Indexer {
    // ==========================================
    // ⚙️ SYSTEM CONFIGURATIONS
    // ==========================================
    private static final String FOLDER_PATH = "C:\\Users\\NITISH JAGANWAR\\Desktop\\test\\chatbot\\files";
    private static final String REGISTRY_FILE = "C:\\Users\\NITISH JAGANWAR\\Desktop\\test\\chatbot\\registry.properties";
    private static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
    private static final String text_embedding_model = "text-embedding-3-large"; 
   // private static final String MILVUS_URL = "http://localhost:19530";
 // PURANA: "http://localhost:19530"
    private static final String MILVUS_URL = "http://204.168.175.115:19530";
    private static final String MILVUS_TOKEN = "root:Milvus";
    private static final String MILVUS_COLLECTION_NAME = "openai_data_collection"; 
    private static final int THREAD_COUNT = 2;
    private static final int BATCH_SIZE = 50;  
    // The Registry object to hold our file states (Memory of the program)
    private static Properties fileRegistry = new Properties();

    public static void main(String[] args) {
        System.out.println("🚀 Smart Enterprise Indexer Started...");
        // STEP 1: Load the memory (Registry) so the code remembers previous runs
        loadRegistry();
        Path documentDirectory = Paths.get(FOLDER_PATH);
        List<Path> allFilePaths = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(documentDirectory)) {
            allFilePaths = paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".txt") || p.toString().toLowerCase().endsWith(".pdf"))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error reading directory: " + e.getMessage());
            return;
        }

        if (allFilePaths.isEmpty()) return;

        System.out.println("🔌 Connecting to OpenAI and Milvus...");
        
        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName(text_embedding_model)
                .build();

        EmbeddingStore<TextSegment> milvusStore = MilvusEmbeddingStore.builder()
                .uri(MILVUS_URL)
                .token(MILVUS_TOKEN)
                .collectionName(MILVUS_COLLECTION_NAME)
                .dimension(3072) 
                .build();

        dev.langchain4j.data.document.DocumentSplitter splitter = DocumentSplitters.recursive(1500, 300);
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < allFilePaths.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, allFilePaths.size());
            List<Path> currentBatchPaths = allFilePaths.subList(i, endIndex);
            int batchNumber = (i / BATCH_SIZE) + 1;
            
            executorService.submit(() -> processBatch(currentBatchPaths, batchNumber, embeddingModel, milvusStore, splitter));
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(2, TimeUnit.HOURS); 
        } catch (InterruptedException e) {
            System.err.println("❌ Interrupted!");
        }
        
        // STEP 2: Save the updated memory (Registry) after all files are processed
        saveRegistry();
        System.out.println("🎉 SUCCESS! Smart Indexing complete.");
    }

    // ==========================================
    // 🧠 CORE LOGIC: Processing files with Hash checking
    // ==========================================
    private static void processBatch(List<Path> batchPaths, int batchNumber, 
                                     EmbeddingModel embeddingModel, 
                                     EmbeddingStore<TextSegment> milvusStore,
                                     dev.langchain4j.data.document.DocumentSplitter splitter) {
        
        String threadName = Thread.currentThread().getName();

        for (Path file : batchPaths) {
            String fileName = file.getFileName().toString();
            
            try {
                // 1. Calculate the digital fingerprint (Hash) of the current file
                String currentFileHash = calculateSHA256(file);
                
                // 2. Check the memory (Registry)
                String savedHash = fileRegistry.getProperty(fileName);
                
                // 3. The Smart Decision Maker
                if (savedHash != null && savedHash.equals(currentFileHash)) {
                    System.out.println("⏭️ [" + threadName + "] Skipped (Already Processed & Unchanged): " + fileName);
                    continue; // Skip to the next file! Saves OpenAI cost and time!
                }
                
                // If we reach here, it means the file is either NEW or MODIFIED.
                System.out.println("⏳ [" + threadName + "] Processing (New/Modified): " + fileName);

                Document docToIngest = null;
                if (fileName.toLowerCase().endsWith(".txt")) {
                    try {
                        String content = new String(Files.readAllBytes(file), java.nio.charset.StandardCharsets.UTF_8);
                        docToIngest = Document.from(content, dev.langchain4j.data.document.Metadata.metadata("file_name", fileName));
                    } catch (Exception encodingError) {
                        String content = new String(Files.readAllBytes(file), java.nio.charset.StandardCharsets.ISO_8859_1);
                        docToIngest = Document.from(content, dev.langchain4j.data.document.Metadata.metadata("file_name", fileName));
                    }
                } else if (fileName.toLowerCase().endsWith(".pdf")) {
                    docToIngest = FileSystemDocumentLoader.loadDocument(file, new ApachePdfBoxDocumentParser());
                }

                if (docToIngest != null) {
                    List<TextSegment> allSegments = splitter.split(docToIngest);
                    
                    int subBatchSize = 100; 
                    for (int j = 0; j < allSegments.size(); j += subBatchSize) {
                        int end = Math.min(j + subBatchSize, allSegments.size());
                        List<TextSegment> smallChunkBatch = allSegments.subList(j, end);
                        
                        List<dev.langchain4j.data.embedding.Embedding> embeddings = embeddingModel.embedAll(smallChunkBatch).content();
                        milvusStore.addAll(embeddings, smallChunkBatch);
                        
                        Thread.sleep(2000); // Rate limiting pause
                    }
                    
                    // 4. Update the Registry with the new successful hash!
                    // Synchronized to prevent multiple threads from corrupting the registry map
                    synchronized (fileRegistry) {
                        fileRegistry.setProperty(fileName, currentFileHash);
                    }
                    
                    System.out.println("✅ [" + threadName + "] Successfully Ingested: " + fileName);
                }

            } catch (Exception e) {
                System.err.println("🚨 [" + threadName + "] Failed to ingest '" + fileName + "' -> " + e.getMessage());
            }
        }
    }

    // ==========================================
    // 🛠️ UTILITY FUNCTIONS
    // ==========================================
    
    // Calculates the SHA-256 fingerprint of a file
    private static String calculateSHA256(Path filePath) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream is = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] hashBytes = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // Loads the properties file from disk into the Java object
    private static void loadRegistry() {
        File file = new File(REGISTRY_FILE);
        if (file.exists()) {
            try (InputStream input = new FileInputStream(file)) {
                fileRegistry.load(input);
            } catch (IOException ex) {
                System.err.println("Warning: Could not load registry. Starting fresh.");
            }
        }
    }

    // Saves the Java object back to the disk as a physical file
    private static void saveRegistry() {
        try (OutputStream output = new FileOutputStream(REGISTRY_FILE)) {
            fileRegistry.store(output, "Milvus Indexer File Hash Registry");
        } catch (IOException ex) {
            System.err.println("Warning: Could not save registry.");
        }
    }
}