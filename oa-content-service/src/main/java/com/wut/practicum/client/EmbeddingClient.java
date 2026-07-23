package com.wut.practicum.client;

import com.wut.practicum.config.RagConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class EmbeddingClient {
    private static final Logger log = LoggerFactory.getLogger(EmbeddingClient.class);
    private static final String QUERY_PREFIX = "为这个句子生成表示以用于检索相关文章：";

    private final RagConfig config;
    private WebClient webClient;

    public EmbeddingClient(RagConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl(config.getEmbedding().getBaseUrl())
                .build();
    }

    public List<Double> embedDocument(String text) {
        return embed(text);
    }

    public List<Double> embedQuery(String question) {
        return embed(QUERY_PREFIX + question);
    }

    /** 批量文档向量化 */
    @SuppressWarnings("unchecked")
    public List<List<Double>> embedDocuments(List<String> texts) {
        if (texts.isEmpty()) return List.of();
        Map<String, Object> body = Map.of("inputs", texts, "normalize", true);
        List<List<Double>> result = webClient.post()
                .uri("/embed")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(List.class)
                .map(r -> (List<List<Double>>) r)
                .block(Duration.ofSeconds(60));

        if (result == null || result.size() != texts.size()) {
            throw new IllegalStateException("批量向量化返回数量不匹配: " +
                    (result != null ? result.size() : 0) + " vs " + texts.size());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Double> embed(String text) {
        Map<String, Object> body = Map.of("inputs", text, "normalize", true);
        List<List<Double>> result = webClient.post()
                .uri("/embed")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(List.class)
                .map(r -> (List<List<Double>>) r)
                .block(Duration.ofSeconds(30));

        if (result == null || result.isEmpty()) {
            throw new IllegalStateException("Embedding服务未返回向量");
        }
        List<Double> vector = result.get(0);
        if (vector.size() != config.getEmbedding().getDimension()) {
            throw new IllegalStateException("向量维度错误: " + vector.size());
        }
        return vector;
    }
}
