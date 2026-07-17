package com.ltss.integration.email.template;

import com.ltss.config.auth.AccountProperties;
import com.ltss.integration.email.AccountEmailEvent;
import com.ltss.integration.email.AccountEmailType;
import org.springframework.stereotype.Component;

@Component
public class VerifyEmailTemplate extends AbstractLinkAccountEmailTemplate {
    public VerifyEmailTemplate(AccountProperties properties) {
        super(properties);
    }

    @Override public AccountEmailType type() { return AccountEmailType.VERIFY_EMAIL; }
    @Override public String subject() { return "Xác minh tài khoản LTSS"; }
    @Override public String body(AccountEmailEvent event) { return "Xác minh email bằng liên kết sau:\n" + link("/verify-email", event.rawToken()); }
}
