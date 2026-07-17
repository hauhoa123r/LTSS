package com.ltss.features.auth.controller;

import com.ltss.common.logging.RequestIdFilter;
import com.ltss.features.auth.service.ClientRequestInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class ClientRequestInfoFactory {
    public ClientRequestInfo from(HttpServletRequest request) {
        return new ClientRequestInfo(
                request.getRemoteAddr(),
                MDC.get(RequestIdFilter.MDC_KEY)
        );
    }
}
