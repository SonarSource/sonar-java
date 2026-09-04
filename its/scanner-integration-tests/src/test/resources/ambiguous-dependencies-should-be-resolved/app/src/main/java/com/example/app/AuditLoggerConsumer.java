package com.example.app;

import com.example.common.AuditLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * CASE: ambiguity resolved via an unprofiled @Primary despite a competing profiled @Primary, cross-module.
 * DefaultAuditLogger (@Primary) lives in module-a, StagingAuditLogger (@Primary + @Profile("staging"))
 * in module-b. Excluding the profiled candidate leaves DefaultAuditLogger as the unique primary.
 */
@Component
public class AuditLoggerConsumer {

    @Autowired
    private AuditLogger auditLogger;
}
