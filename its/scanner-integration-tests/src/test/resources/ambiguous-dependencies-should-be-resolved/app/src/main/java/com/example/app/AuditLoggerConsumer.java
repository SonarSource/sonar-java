package com.example.app;

import com.example.common.AuditLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * CASE: two @Primary candidates colliding under an active profile, cross-module.
 * DefaultAuditLogger (@Primary) lives in module-a, StagingAuditLogger (@Primary + @Profile("staging"))
 * in module-b. When the "staging" profile is active both are primary, so neither wins.
 */
@Component
public class AuditLoggerConsumer {

    @Autowired
    private AuditLogger auditLogger;
}
