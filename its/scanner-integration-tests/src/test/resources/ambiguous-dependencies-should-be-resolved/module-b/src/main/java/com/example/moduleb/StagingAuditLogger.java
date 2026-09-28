package com.example.moduleb;

import com.example.common.AuditLogger;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Primary
@Profile("staging")
@Component
public class StagingAuditLogger implements AuditLogger {
    @Override
    public void log(String event) {
        // not needed for test
    }
}
