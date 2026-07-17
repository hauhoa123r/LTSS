package com.ltss.integration.email.template;

import com.ltss.integration.email.AccountEmailEvent;
import com.ltss.integration.email.AccountEmailType;
import org.springframework.stereotype.Component;

@Component
public class ChangePasswordOtpEmailTemplate implements AccountEmailTemplate {
    @Override public AccountEmailType type() { return AccountEmailType.CHANGE_PASSWORD_OTP; }
    @Override public String subject() { return "Mã xác nhận đổi mật khẩu LTSS"; }
    @Override public String body(AccountEmailEvent event) {
        return "Mã xác nhận đổi mật khẩu của bạn là: " + event.rawToken()
                + "\nMã có hiệu lực trong 10 phút.";
    }
}
