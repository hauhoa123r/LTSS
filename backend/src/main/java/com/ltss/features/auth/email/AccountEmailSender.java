package com.ltss.features.auth.email;

public interface AccountEmailSender {
    void send(AccountEmailEvent event);
}
