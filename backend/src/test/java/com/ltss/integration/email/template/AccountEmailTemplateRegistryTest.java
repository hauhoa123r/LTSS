package com.ltss.integration.email.template;

import com.ltss.config.auth.AccountProperties;
import com.ltss.integration.email.AccountEmailEvent;
import com.ltss.integration.email.AccountEmailType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountEmailTemplateRegistryTest {
    private final AccountProperties properties = new AccountProperties(
            new AccountProperties.Email(true, "noreply@ltss.local", "https://ltss.local"),
            Duration.ofHours(1), Duration.ofDays(1), Duration.ofMinutes(10),
            Duration.ofMinutes(1), Duration.ofMinutes(15), 5
    );

    @Test
    void selectsTemplateByEmailType() {
        VerifyEmailTemplate verify = new VerifyEmailTemplate(properties);
        AccountEmailTemplateRegistry registry = new AccountEmailTemplateRegistry(List.of(
                verify,
                new ResetPasswordEmailTemplate(properties),
                new ChangePasswordOtpEmailTemplate()
        ));

        AccountEmailTemplate selected = registry.require(AccountEmailType.VERIFY_EMAIL);

        assertEquals(verify, selected);
        assertEquals(
                "Xác minh email bằng liên kết sau:\nhttps://ltss.local/verify-email?token=token-value",
                selected.body(new AccountEmailEvent("user@ltss.local", AccountEmailType.VERIFY_EMAIL, "token-value"))
        );
    }

    @Test
    void rejectsDuplicateTemplateRegistration() {
        assertThrows(IllegalStateException.class, () -> new AccountEmailTemplateRegistry(List.of(
                new VerifyEmailTemplate(properties),
                new VerifyEmailTemplate(properties)
        )));
    }
}
