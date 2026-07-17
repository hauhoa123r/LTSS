package com.ltss.integration.email.template;

import com.ltss.integration.email.AccountEmailEvent;
import com.ltss.integration.email.AccountEmailType;

public interface AccountEmailTemplate {
    AccountEmailType type();

    String subject();

    String body(AccountEmailEvent event);
}
