package com.ltss.integration.email.template;

import com.ltss.config.auth.AccountProperties;
import com.ltss.integration.email.AccountEmailEvent;
import com.ltss.integration.email.AccountEmailType;
import org.springframework.stereotype.Component;

@Component
public class ResetPasswordEmailTemplate extends AbstractLinkAccountEmailTemplate {
    public ResetPasswordEmailTemplate(AccountProperties properties) {
        super(properties);
    }

    @Override public AccountEmailType type() { return AccountEmailType.RESET_PASSWORD; }
    @Override public String subject() { return "Đặt lại mật khẩu LTSS"; }
    @Override public String body(AccountEmailEvent event) { return "Đặt lại mật khẩu bằng liên kết sau:\n" + link("/reset-password", event.rawToken()); }
}
