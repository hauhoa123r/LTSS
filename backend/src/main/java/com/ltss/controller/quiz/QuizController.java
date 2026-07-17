package com.ltss.controller.quiz;

import com.ltss.common.response.*;
import com.ltss.controller.auth.ClientRequestInfoFactory;
import com.ltss.dto.auth.response.MessageResponse;
import com.ltss.dto.quiz.*;
import com.ltss.service.quiz.QuizService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
public class QuizController {
    private final QuizService service;
    private final ApiResponseFactory responseFactory;
    private final ClientRequestInfoFactory requestInfoFactory;

    public QuizController(QuizService service, ApiResponseFactory responseFactory,
                          ClientRequestInfoFactory requestInfoFactory) {
        this.service = service;
        this.responseFactory = responseFactory;
        this.requestInfoFactory = requestInfoFactory;
    }

    @GetMapping("/api/v1/quizzes")
    public ApiResponse<PageResponse<QuizSummaryResponse>> published(
            @RequestParam(required = false) @Min(1) Long placeId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size) {
        return responseFactory.success(service.published(placeId, page, size));
    }

    @GetMapping("/api/v1/quizzes/{quizId}")
    public ApiResponse<QuizSummaryResponse> publishedDetail(@PathVariable @Min(1) Long quizId) {
        return responseFactory.success(service.publishedDetail(quizId));
    }

    @GetMapping("/api/v1/management/quizzes")
    public ApiResponse<PageResponse<QuizSummaryResponse>> mine(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return responseFactory.success(service.mine(page, size));
    }

    @GetMapping("/api/v1/management/quizzes/{quizId}")
    public ApiResponse<QuizAuthorResponse> managementDetail(@PathVariable @Min(1) Long quizId) {
        return responseFactory.success(service.managementDetail(quizId));
    }

    @PostMapping("/api/v1/management/quizzes")
    public ApiResponse<QuizAuthorResponse> create(@Valid @RequestBody QuizUpsertRequest request,
                                                   HttpServletRequest httpRequest) {
        return responseFactory.success(service.create(request, requestInfoFactory.from(httpRequest)));
    }

    @PutMapping("/api/v1/management/quizzes/{quizId}")
    public ApiResponse<QuizAuthorResponse> update(@PathVariable @Min(1) Long quizId,
                                                   @Valid @RequestBody QuizUpsertRequest request,
                                                   HttpServletRequest httpRequest) {
        return responseFactory.success(service.update(quizId, request, requestInfoFactory.from(httpRequest)));
    }

    @DeleteMapping("/api/v1/management/quizzes/{quizId}")
    public ApiResponse<MessageResponse> delete(@PathVariable @Min(1) Long quizId,
                                                @RequestParam @Min(0) Integer version,
                                                HttpServletRequest httpRequest) {
        service.delete(quizId, version, requestInfoFactory.from(httpRequest));
        return responseFactory.success(new MessageResponse("Quiz deleted successfully"));
    }
}
