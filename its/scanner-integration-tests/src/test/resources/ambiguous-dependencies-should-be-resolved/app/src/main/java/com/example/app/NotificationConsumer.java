package com.example.app;

import com.example.common.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * CASE: unresolved ambiguity, same module.
 * EmailNotificationService and SmsNotificationService (module-a) are both
 * plain beans with no @Primary/@Qualifier - ambiguous.
 */
@Component
public class NotificationConsumer {

    @Autowired
    private NotificationService notificationService;
}
