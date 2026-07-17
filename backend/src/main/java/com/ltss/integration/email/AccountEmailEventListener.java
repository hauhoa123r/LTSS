package com.ltss.integration.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class AccountEmailEventListener {
    private final AccountEmailSender sender;

    public AccountEmailEventListener(AccountEmailSender sender) {
        this.sender = sender;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccountEmail(AccountEmailEvent event) {
        try {
            sender.send(event);
        } catch (RuntimeException exception) {
            log.error("Failed to deliver account email of type {}", event.type(), exception);
        }
    }
}
