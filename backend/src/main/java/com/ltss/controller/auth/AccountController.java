package com.ltss.controller.auth;

import com.ltss.common.response.ApiResponse;
import com.ltss.common.response.ApiResponseFactory;
import com.ltss.dto.auth.request.ChangePasswordRequest;
import com.ltss.dto.auth.request.UpdateProfileRequest;
import com.ltss.dto.auth.response.MessageResponse;
import com.ltss.dto.auth.response.ProfileResponse;
import com.ltss.service.auth.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account")
public class AccountController {
    private final AccountService accountService;
    private final ClientRequestInfoFactory requestInfoFactory;
    private final ApiResponseFactory responseFactory;

    public AccountController(
            AccountService accountService,
            ClientRequestInfoFactory requestInfoFactory,
            ApiResponseFactory responseFactory
    ) {
        this.accountService = accountService;
        this.requestInfoFactory = requestInfoFactory;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/me")
    public ApiResponse<ProfileResponse> getProfile() {
        return responseFactory.success(accountService.getProfile());
    }

    @PutMapping("/me")
    public ApiResponse<ProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            HttpServletRequest servletRequest
    ) {
        return responseFactory.success(accountService.updateProfile(
                request, requestInfoFactory.from(servletRequest)
        ));
    }

    @PostMapping("/password/change-otp")
    public ApiResponse<MessageResponse> requestChangePasswordOtp(HttpServletRequest servletRequest) {
        return responseFactory.success(accountService.requestChangePasswordOtp(
                requestInfoFactory.from(servletRequest)
        ));
    }

    @PutMapping("/password")
    public ApiResponse<MessageResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest servletRequest
    ) {
        return responseFactory.success(accountService.changePassword(
                request, requestInfoFactory.from(servletRequest)
        ));
    }
}
