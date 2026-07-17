package com.ltss.controller.auth;

import com.ltss.common.response.ApiResponse;
import com.ltss.common.response.ApiResponseFactory;
import com.ltss.dto.auth.request.EmailRequest;
import com.ltss.dto.auth.request.LoginRequest;
import com.ltss.dto.auth.request.RegisterRequest;
import com.ltss.dto.auth.request.ResetPasswordRequest;
import com.ltss.dto.auth.request.TokenRequest;
import com.ltss.dto.auth.response.AuthResponse;
import com.ltss.dto.auth.response.MessageResponse;
import com.ltss.common.exception.auth.AccountException;
import com.ltss.security.auth.RefreshCookieService;
import com.ltss.service.auth.AuthService;
import com.ltss.service.auth.AuthenticatedSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final RefreshCookieService refreshCookieService;
    private final ClientRequestInfoFactory requestInfoFactory;
    private final ApiResponseFactory responseFactory;

    public AuthController(
            AuthService authService,
            RefreshCookieService refreshCookieService,
            ClientRequestInfoFactory requestInfoFactory,
            ApiResponseFactory responseFactory
    ) {
        this.authService = authService;
        this.refreshCookieService = refreshCookieService;
        this.requestInfoFactory = requestInfoFactory;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<MessageResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest
    ) {
        MessageResponse response = authService.register(request, requestInfoFactory.from(servletRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(responseFactory.success(response));
    }

    @PostMapping("/email/verify")
    public ApiResponse<MessageResponse> verifyEmail(
            @Valid @RequestBody TokenRequest request,
            HttpServletRequest servletRequest
    ) {
        return responseFactory.success(authService.verifyEmail(
                request.token(), requestInfoFactory.from(servletRequest)
        ));
    }

    @PostMapping("/email/resend")
    public ApiResponse<MessageResponse> resendVerification(
            @Valid @RequestBody EmailRequest request,
            HttpServletRequest servletRequest
    ) {
        return responseFactory.success(authService.resendVerification(
                request.email(), requestInfoFactory.from(servletRequest)
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        AuthenticatedSession session = authService.login(
                request, requestInfoFactory.from(servletRequest)
        );
        return sessionResponse(session);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = "${ltss.security.refresh-cookie.name}", required = false) String refreshToken,
            HttpServletRequest servletRequest
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw AccountException.invalidToken();
        }
        AuthenticatedSession session = authService.refresh(
                refreshToken, requestInfoFactory.from(servletRequest)
        );
        return sessionResponse(session);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<MessageResponse>> logout(
            @CookieValue(name = "${ltss.security.refresh-cookie.name}", required = false) String refreshToken,
            HttpServletRequest servletRequest
    ) {
        authService.logout(refreshToken, requestInfoFactory.from(servletRequest));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookieService.clear().toString())
                .body(responseFactory.success(new MessageResponse("Logged out successfully")));
    }

    @PostMapping("/password/forgot")
    public ApiResponse<MessageResponse> forgotPassword(
            @Valid @RequestBody EmailRequest request,
            HttpServletRequest servletRequest
    ) {
        return responseFactory.success(authService.forgotPassword(
                request.email(), requestInfoFactory.from(servletRequest)
        ));
    }

    @PostMapping("/password/reset")
    public ApiResponse<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest servletRequest
    ) {
        return responseFactory.success(authService.resetPassword(
                request, requestInfoFactory.from(servletRequest)
        ));
    }

    private ResponseEntity<ApiResponse<AuthResponse>> sessionResponse(AuthenticatedSession session) {
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookieService.create(session.refreshToken()).toString()
                )
                .body(responseFactory.success(session.response()));
    }
}
