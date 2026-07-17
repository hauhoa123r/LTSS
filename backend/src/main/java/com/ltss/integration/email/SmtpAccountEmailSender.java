package com.ltss.integration.email;

import com.ltss.config.auth.AccountProperties;
import com.ltss.integration.email.template.AccountEmailTemplate;
import com.ltss.integration.email.template.AccountEmailTemplateRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmtpAccountEmailSender implements AccountEmailSender {
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AccountProperties properties;
    private final AccountEmailTemplateRegistry templates;

    public SmtpAccountEmailSender(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            AccountProperties properties,
            AccountEmailTemplateRegistry templates
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.properties = properties;
        this.templates = templates;
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
        AccountEmailTemplate template = templates.require(event.type());
        message.setFrom(properties.email().from());
        message.setTo(event.recipient());
        message.setSubject(template.subject());
        message.setText(template.body(event));
        mailSender.send(message);
    }
}
