package com.ltss.integration.email.template;

import com.ltss.integration.email.AccountEmailType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class AccountEmailTemplateRegistry {
    private final Map<AccountEmailType, AccountEmailTemplate> templates;

    public AccountEmailTemplateRegistry(List<AccountEmailTemplate> templates) {
        Map<AccountEmailType, AccountEmailTemplate> indexed = new EnumMap<>(AccountEmailType.class);
        for (AccountEmailTemplate template : templates) {
            if (indexed.put(template.type(), template) != null) {
                throw new IllegalStateException("Duplicate account email template for " + template.type());
            }
        }
        this.templates = Map.copyOf(indexed);
    }

    public AccountEmailTemplate require(AccountEmailType type) {
        AccountEmailTemplate template = templates.get(type);
        if (template == null) {
            throw new IllegalArgumentException("Unsupported account email type: " + type);
        }
        return template;
    }
}
