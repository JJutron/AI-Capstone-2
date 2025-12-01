package com.vegin.module.analysis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegin.auth.UserPrincipal;
import com.vegin.common.ApiResponse;
import com.vegin.domain.SurveyDto;
import com.vegin.module.analysis.AnalysisResultResponse;
import com.vegin.dto.response.AnalysisUploadResponse;
import com.vegin.external.dto.FastApiResponseDto;
import com.vegin.module.analysis.service.SkinAnalysisService;
import com.vegin.module.users.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Tag(name = "Analysis", description = "피부 이미지 분석 API")
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final SkinAnalysisService service;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    /**
     * 실제 서비스용:
     *  - 이미지 S3 업로드
     *  - image_url + surveyJson 을 FastAPI로 전송
     *  - DB에 PENDING → DONE 업데이트
     *  - FE에는 analysisId + imageUrl 반환
     */
    @Operation(
            summary = "피부 이미지 업로드 & 분석 요청",
            description = """
                    사용자 피부 이미지를 업로드하고, 설문 JSON과 함께 FastAPI에 분석을 요청합니다.
                    응답으로는 분석 ID와 이미지 URL이 포함됩니다.
                    이후 마이페이지/히스토리 API에서 상세 분석 결과(JSON)를 조회할 수 있습니다.
                    """,
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @PostMapping(
            value = "/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ApiResponse<AnalysisUploadResponse> uploadAndAnalyze(
            @RequestPart("file") MultipartFile file,
            @RequestPart("survey") SurveyDto survey,
            Authentication auth
    ) {
        Long userId = resolveUserId(auth);

        // SurveyDto를 JSON 문자열로 변환
        String surveyJson = toSurveyJson(survey);
        log.info("[AnalysisController] survey received: {}", surveyJson);

        AnalysisUploadResponse res = service.uploadAndAnalyze(
                userId,
                file,
                surveyJson
        );

        return ApiResponse.ok(res);
    }

    @GetMapping("/{analysisId}")
    @Operation(
            summary = "피부 분석 결과 조회",
            description = "FastAPI 결과 + BST정보 + 유저정보 + DO / DONT 성분 + action 버튼 반환",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    public ApiResponse<AnalysisResultResponse> getResult(
            @PathVariable Long analysisId,
            Authentication auth
    ) {
        Long userId = resolveUserId(auth);
        return ApiResponse.ok(service.getAnalysisResult(analysisId, userId));
    }






    /**
     * (옵션) 개발/테스트용:
     *  - S3, DB 안 거치고 FastAPI에 파일만 바로 보내서 응답 구조 확인
     */
    @Operation(
            summary = "FastAPI 직접 호출 테스트 (파일 전송)",
            description = """
                    개발/테스트용 엔드포인트입니다.
                    S3에 업로드하지 않고, 업로드된 파일을 그대로 FastAPI의 image_file로 전송하여
                    응답 구조(Fusion, recommendations 등)를 확인할 때 사용합니다.
                    실제 서비스 플로우에서는 /api/analysis/image 를 사용하세요.
                    """
    )
    @PostMapping(
            value = "/image/test-direct",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ApiResponse<FastApiResponseDto> testDirectFastApi(
            @RequestPart("file") MultipartFile file,
            @RequestPart("survey") SurveyDto survey
    ) {
        // SurveyDto를 JSON 문자열로 변환
        String surveyJson = toSurveyJson(survey);
        log.info("[AnalysisController] testDirectFastApi - survey received: {}", surveyJson);
        
        FastApiResponseDto dto = service.analyzeFileOnlyForTest(file, surveyJson);
        return ApiResponse.ok(dto);
    }

    // ================== 내부 유틸 ==================

    /**
     * SurveyDto를 JSON 문자열로 변환
     */
    private String toSurveyJson(SurveyDto survey) {
        try {
            return objectMapper.writeValueAsString(survey);
        } catch (Exception e) {
            log.error("[AnalysisController] SurveyDto JSON 변환 실패", e);
            throw new RuntimeException("설문 데이터 변환 실패", e);
        }
    }

    private Long resolveUserId(Authentication auth) {
        Object principal = auth.getPrincipal();

        // 우리가 만든 UserPrincipal 인 경우
        if (principal instanceof UserPrincipal up) {
            return up.getId();
        }

        // 스프링 기본 User인 경우 (JWT 필터에서 org.springframework.security.core.userdetails.User로 올린 케이스)
        if (principal instanceof org.springframework.security.core.userdetails.User u) {
            String email = u.getUsername();
            return userService.getUserIdByEmail(email);
        }

        throw new IllegalStateException("지원하지 않는 principal 타입: " + principal);
    }
}




//package com.vegin.module.analysis.controller;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.vegin.common.ApiResponse;
//import com.vegin.domain.SurveyWrapper;
//import com.vegin.module.analysis.service.SkinAnalysisService;
//import com.vegin.module.users.service.UserService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.security.SecurityRequirement;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.MediaType;
//import org.springframework.security.core.Authentication;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.Map;
//
//@Tag(name = "Analysis", description = "피부 이미지 분석 API")
//@RestController
//@RequestMapping("/api/analysis")
//@RequiredArgsConstructor
//public class AnalysisController {
//    private final SkinAnalysisService service;
//    private final UserService userService;
//
//    @Operation(
//            summary = "피부 이미지 업로드 & 분석 요청",
//            description = """
//                    사용자 피부 이미지를 업로드하고, 설문 JSON과 함께 FastAPI에 분석을 요청합니다.
//                    응답으로는 Fusion(지표, MBTI) + 추천 제품 목록 등이 포함된 결과를 반환합니다.
//                    """,
//            security = { @SecurityRequirement(name = "bearerAuth") }
//    )
//
//    @PostMapping(value ="/image",
//            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ApiResponse<?> uploadAndAnalyze(
//            @RequestPart("file") MultipartFile file,
//            @RequestPart("survey") String surveyJson,
//            Authentication auth
//    ) {
//        Long userId = resolveUserId(auth);
//
//        // SurveyWrapper → JSON 문자열
////        String surveyJson = toSurveyJson(wrapper);
//
//        return ApiResponse.ok(
//                service.uploadAndAnalyze(
//                        userId,
//                        file,
//                        surveyJson
//                )
//        );
//    }
//
//    private Long resolveUserId(Authentication auth) {
//        Object principal = auth.getPrincipal();
//
//        if (principal instanceof com.vegin.auth.UserPrincipal up) {
//            return up.getId();
//        }
//
//        // 지금 실제로 들어와 있는 org.springframework.security.core.userdetails.User 인 경우
//        if (principal instanceof org.springframework.security.core.userdetails.User u) {
//            String email = u.getUsername();
//            return userService.getUserIdByEmail(email);
//        }
//
//        throw new IllegalStateException("지원하지 않는 principal 타입: " + principal);
//
//    }
//    private String toSurveyJson(SurveyWrapper wrapper) {
//        try {
//            ObjectMapper mapper = new ObjectMapper();
//            // {"survey": { q1:..., q2:... }}
//            return mapper.writeValueAsString(Map.of("survey", wrapper.survey()));
//        } catch (Exception e) {
//            throw new RuntimeException("설문 JSON 변환 실패", e);
//}}}
