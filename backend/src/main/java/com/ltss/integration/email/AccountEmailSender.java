package com.ltss.integration.email;

public interface AccountEmailSender {
    void send(AccountEmailEvent event);
}
