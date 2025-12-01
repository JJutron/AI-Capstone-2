package com.vegin.module.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegin.common.S3Service;
import com.vegin.dto.response.AnalysisUploadResponse;
import com.vegin.external.dto.FastApiResponseDto;
import com.vegin.external.service.FastApiClient;
import com.vegin.module.analysis.domain.SkinAnalysis;
import com.vegin.module.analysis.repository.SkinAnalysisRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SkinAnalysisService 단위 테스트")
class SkinAnalysisServiceTest {

    @Mock
    private SkinAnalysisRepository analyses;

    @Mock
    private S3Service s3Service;

    @Mock
    private FastApiClient fastApiClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private SkinAnalysisService skinAnalysisService;

    // Reflection을 사용하여 SkinAnalysis의 id 필드 설정 헬퍼 메서드
    private void setId(SkinAnalysis analysis, Long id) {
        try {
            Field idField = SkinAnalysis.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(analysis, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID", e);
        }
    }

    @Test
    @DisplayName("TC-B01-N01: (정상) 유효한 이미지와 설문으로 분석 요청 성공")
    void uploadAndAnalyze_ValidRequest_Success() throws Exception {
        // Given
        Long userId = 1L;
        String surveyJson = "{\"q1\":\"1\",\"q2\":\"2\",\"q3\":\"3\",\"q4\":\"4\",\"q5\":\"5\",\"q6\":\"1\",\"q7\":\"2\",\"q8\":\"3\",\"q9\":\"4\",\"q10\":\"1\"}";
        String imageUrl = "https://vegin-media-submit.s3.ap-northeast-2.amazonaws.com/analysis/1/1234567890_abc12345.jpg";

        // MultipartFile Mock 설정
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("skin.jpg");

        // S3 업로드 Mock
        doNothing().when(s3Service).upload(any(MultipartFile.class), anyString());
        when(s3Service.getUrl(anyString())).thenReturn(imageUrl);

        // ArgumentCaptor로 실제 저장된 객체 캡처
        ArgumentCaptor<SkinAnalysis> analysisCaptor = ArgumentCaptor.forClass(SkinAnalysis.class);

        // DB 저장 Mock - 두 번 호출되므로 동일한 Answer 사용
        when(analyses.save(analysisCaptor.capture())).thenAnswer(invocation -> {
            SkinAnalysis analysis = invocation.getArgument(0);
            if (analysis.getId() == null) {
                setId(analysis, 100L);
            }
            return analysis;
        });

        // FastAPI 응답 Mock
        Map<String, Object> fusion = new HashMap<>();
        fusion.put("skin_type", "건성");
        fusion.put("skin_mbti", "DSPW");
        fusion.put("indices", new HashMap<>());
        fusion.put("vision_raw", new HashMap<>());

        FastApiResponseDto fastApiResponse = new FastApiResponseDto(
                "success",
                fusion,
                List.of(),
                null
        );

        when(fastApiClient.analyzeWithImageUrl(anyString(), anyString()))
                .thenReturn(fastApiResponse);

        // ObjectMapper Mock - JSON 변환
        when(objectMapper.writeValueAsString(any(FastApiResponseDto.class)))
                .thenReturn("{\"status\":\"success\",\"fusion\":{}}");

        // When
        AnalysisUploadResponse response = skinAnalysisService.uploadAndAnalyze(
                userId,
                multipartFile,
                surveyJson
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.analysisId()).isEqualTo(100L);
        assertThat(response.s3Url()).isEqualTo(imageUrl);

        // 검증: S3 업로드 호출 확인
        verify(s3Service, times(1)).upload(any(MultipartFile.class), anyString());
        verify(s3Service, times(1)).getUrl(anyString());

        // 검증: DB 저장이 2번 호출되었는지 확인
        verify(analyses, times(2)).save(any(SkinAnalysis.class));

        // 검증: ArgumentCaptor로 캡처된 모든 호출 검증
        List<SkinAnalysis> savedAnalyses = analysisCaptor.getAllValues();
        assertThat(savedAnalyses).hasSize(2);

        // 첫 번째 저장: PENDING 상태
        SkinAnalysis pendingAnalysis = savedAnalyses.get(0);
        assertThat(pendingAnalysis.getStatus()).isEqualTo("PENDING");
        assertThat(pendingAnalysis.getUserId()).isEqualTo(userId);
        assertThat(pendingAnalysis.getS3Key()).isNotNull();
        assertThat(pendingAnalysis.getS3Key()).startsWith("analysis/1/");
        assertThat(pendingAnalysis.getUserInput()).isEqualTo(surveyJson);
        assertThat(pendingAnalysis.getResult()).isNull();

        // 두 번째 저장: DONE 상태
        SkinAnalysis doneAnalysis = savedAnalyses.get(1);
        assertThat(doneAnalysis.getStatus()).isEqualTo("DONE");
        assertThat(doneAnalysis.getResult()).isNotNull();
        assertThat(doneAnalysis.getId()).isEqualTo(100L);
        assertThat(doneAnalysis.getUserId()).isEqualTo(userId);

        // 검증: FastAPI 호출 확인
        verify(fastApiClient, times(1)).analyzeWithImageUrl(imageUrl, surveyJson);
    }

    @Test
    @DisplayName("TC-B02-E01: (예외) S3 업로드 실패 시 롤백")
    void uploadAndAnalyze_S3UploadFailure_Rollback() {
        // Given
        Long userId = 1L;
        String surveyJson = "{\"q1\":\"1\",\"q2\":\"2\"}";

        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("skin.jpg");

        // S3 업로드 실패 시뮬레이션
        doThrow(new ResponseStatusException(
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                "S3 service error"
        )).when(s3Service).upload(any(MultipartFile.class), anyString());

        // When & Then
        assertThatThrownBy(() -> skinAnalysisService.uploadAndAnalyze(
                userId,
                multipartFile,
                surveyJson
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("S3 service error");

        // 검증: S3 업로드 시도 확인
        verify(s3Service, times(1)).upload(any(MultipartFile.class), anyString());

        // 검증: DB 저장은 호출되지 않음 (트랜잭션 롤백)
        verify(analyses, never()).save(any(SkinAnalysis.class));

        // 검증: FastAPI 호출은 하지 않음
        verify(fastApiClient, never()).analyzeWithImageUrl(anyString(), anyString());
    }
}