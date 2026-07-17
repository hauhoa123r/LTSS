package com.ltss.service.auth;

import com.ltss.dto.auth.request.LoginRequest;
import com.ltss.dto.auth.request.RegisterRequest;
import com.ltss.dto.auth.request.ResetPasswordRequest;
import com.ltss.dto.auth.response.MessageResponse;

public interface AuthService {

    MessageResponse register(RegisterRequest request, ClientRequestInfo requestInfo);

    MessageResponse verifyEmail(String rawToken, ClientRequestInfo requestInfo);

    MessageResponse resendVerification(String rawEmail, ClientRequestInfo requestInfo);

    AuthenticatedSession login(LoginRequest request, ClientRequestInfo requestInfo);

    AuthenticatedSession refresh(String rawRefreshToken, ClientRequestInfo requestInfo);

    void logout(String rawRefreshToken, ClientRequestInfo requestInfo);

    MessageResponse forgotPassword(String rawEmail, ClientRequestInfo requestInfo);

    MessageResponse resetPassword(ResetPasswordRequest request, ClientRequestInfo requestInfo);
}
