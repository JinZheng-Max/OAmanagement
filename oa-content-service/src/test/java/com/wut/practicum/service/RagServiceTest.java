package com.wut.practicum.service;

import com.wut.practicum.client.ChromaClient;
import com.wut.practicum.client.DeepSeekClient;
import com.wut.practicum.client.EmbeddingClient;
import com.wut.practicum.config.RagConfig;
import com.wut.practicum.entity.OaAiSession;
import com.wut.practicum.entity.OaAiSource;
import com.wut.practicum.mapper.OaAiCitationMapper;
import com.wut.practicum.mapper.OaAiSessionMapper;
import com.wut.practicum.mapper.OaAiSourceChunkMapper;
import com.wut.practicum.mapper.OaAiSourceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RAG 知识库与 AI 问答单元测试 (UT-10 / UT-11)
 * 依据 《智办AI OA 单元测试模块内容说明书 V2.0》编写
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UT-11: RAG 检索增强生成与 AI 问答单元测试")
public class RagServiceTest {

    @Mock
    private EmbeddingClient embeddingClient;

    @Mock
    private ChromaClient chromaClient;

    @Mock
    private DeepSeekClient deepSeekClient;

    @Mock
    private OaAiSourceMapper aiSourceMapper;

    @Mock
    private OaAiSourceChunkMapper aiSourceChunkMapper;

    @Mock
    private OaAiSessionMapper aiSessionMapper;

    @Mock
    private OaAiCitationMapper aiCitationMapper;

    @Mock
    private RagConfig ragConfig;

    @InjectMocks
    private RagService ragService;

    @BeforeEach
    void setUp() {
        RagConfig.ChromaConfig chroma = new RagConfig.ChromaConfig();
        chroma.setRetrieveCount(5);
        lenient().when(ragConfig.getChroma()).thenReturn(chroma);
    }

    @Nested
    @DisplayName("AI 问答交互与权限防越权校验测试")
    class AiChatTests {

        @Test
        @DisplayName("UT-11-01: 当 Chroma 检索为空时返回友好防幻觉提示，不盲目生成答案")
        void shouldReturnEmptyNoticeWhenNoVectorRecalled() {
            when(embeddingClient.embedQuery("年假天数规定")).thenReturn(List.of(0.1, 0.2));
            when(chromaClient.query(anyList(), anyMap(), anyInt()))
                    .thenReturn(new ChromaClient.ChromaQueryResult(List.of(), List.of(), List.of(), List.of()));

            RagService.AnswerResult result = ragService.ask("年假天数规定", 1L, "EMPLOYEE", 10L);

            assertThat(result).isNotNull();
            assertThat(result.answer()).contains("知识库中暂无相关内容");
            verify(deepSeekClient, never()).chat(anyString(), anyString());
            verify(aiSessionMapper).insert(any(OaAiSession.class));
        }

        @Test
        @DisplayName("UT-11-02: 正常召回授权知识切片后成功组装上下文并调用 DeepSeek 生成解答")
        void shouldGenerateAnswerWithAuthorizedContext() {
            String question = "公司考勤签到时间范围";
            when(embeddingClient.embedQuery(question)).thenReturn(List.of(0.5, 0.8));

            ChromaClient.ChromaQueryResult chromaRes = new ChromaClient.ChromaQueryResult(
                    List.of("vec_001"),
                    List.of("考勤签到开放时间为08:50至12:10"),
                    List.of(Map.of("source_id", 100L)),
                    List.of(0.1)
            );
            when(chromaClient.query(anyList(), anyMap(), anyInt())).thenReturn(chromaRes);

            OaAiSource source = new OaAiSource();
            source.setId(100L);
            source.setTitle("公司考勤制度.pdf");
            source.setVersion(1);
            when(aiSourceMapper.selectAuthorizedSources(anyList(), eq("EMPLOYEE"), anyInt(), eq(10L)))
                    .thenReturn(List.of(source));

            when(deepSeekClient.chat(eq(question), anyString()))
                    .thenReturn("根据《公司考勤制度.pdf》，签到时间为08:50至12:10。");

            RagService.AnswerResult result = ragService.ask(question, 1L, "EMPLOYEE", 10L);

            assertThat(result).isNotNull();
            assertThat(result.answer()).contains("08:50至12:10");
            assertThat(result.sources()).hasSize(1);
            verify(aiCitationMapper).insertBatch(anyList());
        }
    }
}
