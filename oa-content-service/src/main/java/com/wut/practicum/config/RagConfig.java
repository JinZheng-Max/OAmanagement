package com.wut.practicum.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "rag")
public class RagConfig {
    private EmbeddingConfig embedding = new EmbeddingConfig();
    private ChromaConfig chroma = new ChromaConfig();
    private DeepseekConfig deepseek = new DeepseekConfig();
    private ChunkConfig chunk = new ChunkConfig();

    // Getters/setters
    public EmbeddingConfig getEmbedding() { return embedding; }
    public void setEmbedding(EmbeddingConfig embedding) { this.embedding = embedding; }
    public ChromaConfig getChroma() { return chroma; }
    public void setChroma(ChromaConfig chroma) { this.chroma = chroma; }
    public DeepseekConfig getDeepseek() { return deepseek; }
    public void setDeepseek(DeepseekConfig deepseek) { this.deepseek = deepseek; }
    public ChunkConfig getChunk() { return chunk; }
    public void setChunk(ChunkConfig chunk) { this.chunk = chunk; }

    public static class EmbeddingConfig {
        private String baseUrl = "http://localhost:8080";
        private String model = "BAAI/bge-small-zh-v1.5";
        private int dimension = 512;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getDimension() { return dimension; }
        public void setDimension(int dimension) { this.dimension = dimension; }
    }

    public static class ChromaConfig {
        private String baseUrl = "http://localhost:8000";
        private String tenant = "default_tenant";
        private String database = "default_database";
        private String collection = "enterprise_knowledge";
        private int retrieveCount = 15;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getTenant() { return tenant; }
        public void setTenant(String tenant) { this.tenant = tenant; }
        public String getDatabase() { return database; }
        public void setDatabase(String database) { this.database = database; }
        public String getCollection() { return collection; }
        public void setCollection(String collection) { this.collection = collection; }
        public int getRetrieveCount() { return retrieveCount; }
        public void setRetrieveCount(int retrieveCount) { this.retrieveCount = retrieveCount; }
    }

    public static class DeepseekConfig {
        private String baseUrl = "https://api.deepseek.com";
        private String apiKey;
        private String model = "deepseek-chat";
        private int timeoutSeconds = 90;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }

    public static class ChunkConfig {
        private int size = 500;
        private int overlap = 80;

        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        public int getOverlap() { return overlap; }
        public void setOverlap(int overlap) { this.overlap = overlap; }
    }

    public static class StorageConfig {
        private String uploadDir = "./data/ai-uploads";

        public String getUploadDir() {
            // 确保返回绝对路径
            File dir = new File(uploadDir);
            if (!dir.isAbsolute()) {
                return dir.getAbsolutePath();
            }
            return uploadDir;
        }

        public void setUploadDir(String uploadDir) { this.uploadDir = uploadDir; }
    }

    private StorageConfig storage = new StorageConfig();

    public StorageConfig getStorage() { return storage; }
    public void setStorage(StorageConfig storage) { this.storage = storage; }
}
