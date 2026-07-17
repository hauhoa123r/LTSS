package com.ltss.service.administration;

import com.ltss.common.response.PageResponse;
import com.ltss.dto.administration.AuditLogResponse;
import com.ltss.dto.administration.AuditMetadataResponse;
import com.ltss.dto.administration.model.*;
import com.ltss.repository.auth.*;
import java.time.*;
import java.util.*;

public interface AuditQueryService {

    PageResponse<AuditLogResponse> search(Long actorId, String action, String entityType, Long entityId,
                                                 LocalDate from, LocalDate to, int page, int size);

    AuditMetadataResponse metadata();

    AuditLogResponse detail(Long auditLogId);
}
