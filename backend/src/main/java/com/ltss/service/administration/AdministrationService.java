package com.ltss.service.administration;

import com.ltss.common.exception.*;
import com.ltss.common.response.PageResponse;
import com.ltss.dto.administration.*;
import com.ltss.dto.auth.response.MessageResponse;
import com.ltss.entity.auth.*;
import com.ltss.repository.auth.*;
import com.ltss.service.auth.*;
import java.util.*;

public interface AdministrationService {

    PageResponse<AdminUserResponse> users(String query, UserStatus status, int page, int size);

    AdminUserResponse user(Long userId);

    AdminUserResponse updateUser(Long userId, UpdateAdminUserRequest request,
                                  ClientRequestInfo requestInfo);

    AdminUserResponse changeStatus(Long userId, ChangeUserStatusRequest request,
                                          ClientRequestInfo requestInfo);

    AdminUserResponse assignRole(Long userId, String roleCode, RoleChangeRequest request,
                                        ClientRequestInfo requestInfo);

    AdminUserResponse revokeRole(Long userId, String roleCode, RoleChangeRequest request,
                                        ClientRequestInfo requestInfo);

    MessageResponse resetPassword(Long userId, ResetUserPasswordRequest request,
                                  ClientRequestInfo requestInfo);
}
