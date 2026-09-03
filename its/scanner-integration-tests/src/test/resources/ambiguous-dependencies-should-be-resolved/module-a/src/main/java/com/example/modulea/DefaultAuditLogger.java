package com.example.modulea;

import com.example.common.AuditLogger;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class DefaultAuditLogger implements AuditLogger {
    @Override
    public void log(String event) {
        // not needed for test
    }
}
