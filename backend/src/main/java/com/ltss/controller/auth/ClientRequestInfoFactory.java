package com.ltss.controller.auth;

import com.ltss.common.logging.RequestIdFilter;
import com.ltss.service.auth.ClientRequestInfo;
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
