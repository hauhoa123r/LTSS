package com.ltss.features.auth.email;

import com.ltss.features.auth.config.AccountProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class SmtpAccountEmailSender implements AccountEmailSender {
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AccountProperties properties;

    public SmtpAccountEmailSender(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            AccountProperties properties
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.properties = properties;
    }

    @Override
    public void send(AccountEmailEvent event) {
        if (!properties.email().enabled()) {
            log.info("Account email delivery is disabled; message type {} was not sent", event.type());
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.error("Account email delivery is enabled but no JavaMailSender is configured");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.email().from());
        message.setTo(event.recipient());
        message.setSubject(subject(event.type()));
        message.setText(body(event));
        mailSender.send(message);
    }

    private String subject(AccountEmailType type) {
        return switch (type) {
            case VERIFY_EMAIL -> "Xác minh tài khoản LTSS";
            case RESET_PASSWORD -> "Đặt lại mật khẩu LTSS";
            case CHANGE_PASSWORD_OTP -> "Mã xác nhận đổi mật khẩu LTSS";
        };
    }

    private String body(AccountEmailEvent event) {
        return switch (event.type()) {
            case VERIFY_EMAIL -> "Xác minh email bằng liên kết sau:\n" + link("/verify-email", event.rawToken());
            case RESET_PASSWORD -> "Đặt lại mật khẩu bằng liên kết sau:\n" + link("/reset-password", event.rawToken());
            case CHANGE_PASSWORD_OTP -> "Mã xác nhận đổi mật khẩu của bạn là: " + event.rawToken()
                    + "\nMã có hiệu lực trong 10 phút.";
        };
    }

    private String link(String path, String token) {
        return UriComponentsBuilder.fromUriString(properties.email().frontendBaseUrl())
                .path(path)
                .queryParam("token", token)
                .build()
                .encode()
                .toUriString();
    }
}
