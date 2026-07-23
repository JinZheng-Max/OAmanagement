package com.wut.practicum.client;

import com.wut.practicum.config.RagConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.*;

@Service
public class ChromaClient {
    private static final Logger log = LoggerFactory.getLogger(ChromaClient.class);

    private final RagConfig config;
    private WebClient webClient;
    private String collectionId;

    public ChromaClient(RagConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl(config.getChroma().getBaseUrl())
                .build();
        initCollection();
    }

    private String collectionPath() {
        return String.format("/api/v2/tenants/%s/databases/%s/collections",
                config.getChroma().getTenant(), config.getChroma().getDatabase());
    }

    @SuppressWarnings("unchecked")
    public void initCollection() {
        String name = config.getChroma().getCollection();
        // 尝试获取已有集合
        List<Map<String, Object>> existing = webClient.get()
                .uri(collectionPath())
                .retrieve()
                .bodyToMono(List.class)
                .block(Duration.ofSeconds(10));

        if (existing != null) {
            for (Map<String, Object> c : existing) {
                if (name.equals(c.get("name"))) {
                    collectionId = (String) c.get("id");
                    log.info("使用已有Chroma集合: {} id={}", name, collectionId);
                    return;
                }
            }
        }

        // 创建新集合
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("get_or_create", true);
        body.put("configuration", Map.of("hnsw", Map.of("space", "cosine", "ef_construction", 200)));

        Map<String, Object> result = webClient.post()
                .uri(collectionPath())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block(Duration.ofSeconds(10));

        if (result != null) {
            collectionId = (String) result.get("id");
            log.info("创建Chroma集合: {} id={}", name, collectionId);
        }
    }

    public String getCollectionId() {
        return collectionId;
    }

    /** 添加向量 */
    public void addChunk(String vectorId, String content, List<Double> embedding, Map<String, Object> metadata) {
        Map<String, Object> body = new HashMap<>();
        body.put("ids", List.of(vectorId));
        body.put("documents", List.of(content));
        body.put("embeddings", List.of(embedding));
        body.put("metadatas", List.of(metadata));

        webClient.post()
                .uri(collectionPath() + "/" + collectionId + "/add")
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block(Duration.ofSeconds(30));
    }

    /** 批量添加向量 */
    public void addChunks(List<String> ids, List<String> contents, List<List<Double>> embeddings, List<Map<String, Object>> metadatas) {
        Map<String, Object> body = new HashMap<>();
        body.put("ids", ids);
        body.put("documents", contents);
        body.put("embeddings", embeddings);
        body.put("metadatas", metadatas);

        webClient.post()
                .uri(collectionPath() + "/" + collectionId + "/add")
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block(Duration.ofSeconds(60));
    }

    /** 查询相似向量 */
    @SuppressWarnings("unchecked")
    public ChromaQueryResult query(List<Double> queryEmbedding, Map<String, Object> where, int nResults) {
        Map<String, Object> body = new HashMap<>();
        body.put("query_embeddings", List.of(queryEmbedding));
        body.put("n_results", nResults);
        if (where != null) body.put("where", where);
        body.put("include", List.of("documents", "metadatas", "distances"));

        Map<String, Object> result = webClient.post()
                .uri(collectionPath() + "/" + collectionId + "/query")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block(Duration.ofSeconds(30));

        return ChromaQueryResult.fromMap(result);
    }

    /** 删除向量 */
    public void deleteChunks(List<String> ids) {
        Map<String, Object> body = new HashMap<>();
        body.put("ids", ids);
        try {
            webClient.post()
                    .uri(collectionPath() + "/" + collectionId + "/delete")
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofSeconds(30));
        } catch (Exception e) {
            log.warn("Chroma删除失败: {}", e.getMessage());
        }
    }

    public record ChromaQueryResult(List<String> ids, List<String> documents, List<Map<String, Object>> metadatas, List<Double> distances) {
        @SuppressWarnings("unchecked")
        public static ChromaQueryResult fromMap(Map<String, Object> map) {
            if (map == null) return new ChromaQueryResult(List.of(), List.of(), List.of(), List.of());
            List<List<String>> idsRaw = (List<List<String>>) map.get("ids");
            List<List<String>> docsRaw = (List<List<String>>) map.get("documents");
            List<List<Map<String, Object>>> metasRaw = (List<List<Map<String, Object>>>) map.get("metadatas");
            List<List<Double>> distsRaw = (List<List<Double>>) map.get("distances");

            List<String> ids = idsRaw != null && !idsRaw.isEmpty() ? idsRaw.get(0) : List.of();
            List<String> docs = docsRaw != null && !docsRaw.isEmpty() ? docsRaw.get(0) : List.of();
            List<Map<String, Object>> metas = metasRaw != null && !metasRaw.isEmpty() ? metasRaw.get(0) : List.of();
            List<Double> dists = distsRaw != null && !distsRaw.isEmpty() ? distsRaw.get(0) : List.of();

            return new ChromaQueryResult(ids, docs, metas, dists);
        }
    }
}
