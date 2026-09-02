package com.example.modulea;

import com.example.common.NotificationService;
import org.springframework.stereotype.Component;

@Component
public class SmsNotificationService implements NotificationService {
    @Override
    public void send(String message) {
        System.out.println("SMS: " + message);
    }
}
