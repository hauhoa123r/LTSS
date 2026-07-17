package com.ltss.controller.quiz;

import com.ltss.common.response.*;
import com.ltss.controller.auth.ClientRequestInfoFactory;
import com.ltss.dto.quiz.*;
import com.ltss.service.quiz.QuizAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
public class QuizAttemptController {
    private final QuizAttemptService service;
    private final ApiResponseFactory responseFactory;
    private final ClientRequestInfoFactory requestInfoFactory;

    public QuizAttemptController(QuizAttemptService service, ApiResponseFactory responseFactory,
                                 ClientRequestInfoFactory requestInfoFactory) {
        this.service = service;
        this.responseFactory = responseFactory;
        this.requestInfoFactory = requestInfoFactory;
    }

    @PostMapping("/api/v1/quizzes/{quizId}/attempts")
    public ApiResponse<QuizAttemptResponse> start(@PathVariable @Min(1) Long quizId,
                                                   @Valid @RequestBody StartAttemptRequest request,
                                                   HttpServletRequest httpRequest) {
        return responseFactory.success(service.start(quizId, request, requestInfoFactory.from(httpRequest)));
    }

    @GetMapping("/api/v1/quiz-attempts/{attemptId}")
    public ApiResponse<QuizAttemptResponse> detail(@PathVariable @Min(1) Long attemptId,
                                                    HttpServletRequest httpRequest) {
        return responseFactory.success(service.detail(attemptId, requestInfoFactory.from(httpRequest)));
    }

    @PostMapping("/api/v1/quiz-attempts/{attemptId}/submit")
    public ApiResponse<QuizAttemptResponse> submit(@PathVariable @Min(1) Long attemptId,
                                                    @Valid @RequestBody SubmitAttemptRequest request,
                                                    HttpServletRequest httpRequest) {
        return responseFactory.success(service.submit(attemptId, request, requestInfoFactory.from(httpRequest)));
    }

    @GetMapping("/api/v1/account/quiz-attempts")
    public ApiResponse<PageResponse<QuizAttemptSummaryResponse>> history(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return responseFactory.success(service.history(page, size));
    }

    @GetMapping("/api/v1/account/badges")
    public ApiResponse<PageResponse<AwardedBadgeResponse>> badges(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return responseFactory.success(service.badges(page, size));
    }
}
