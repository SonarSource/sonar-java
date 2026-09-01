package com.example.app;

import com.example.common.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

// CASE: ambiguity resolved via @Qualifier, same module.
@Component
public class NotificationQualifiedConsumer {

    @Autowired
    @Qualifier("smsNotificationService")
    private NotificationService notificationService;
}
