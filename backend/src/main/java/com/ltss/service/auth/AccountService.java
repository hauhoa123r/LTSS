package com.ltss.service.auth;

import com.ltss.dto.auth.request.ChangePasswordRequest;
import com.ltss.dto.auth.request.UpdateProfileRequest;
import com.ltss.dto.auth.response.MessageResponse;
import com.ltss.dto.auth.response.ProfileResponse;

public interface AccountService {

    ProfileResponse getProfile();

    ProfileResponse updateProfile(UpdateProfileRequest request, ClientRequestInfo requestInfo);

    MessageResponse requestChangePasswordOtp(ClientRequestInfo requestInfo);

    MessageResponse changePassword(
            ChangePasswordRequest request,
            ClientRequestInfo requestInfo
    );
}
