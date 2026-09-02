package com.example.app;

import com.example.common.ReportingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * CASE: two beans both marked @Primary, same module - still ambiguous.
 * Having more than one "primary" candidate is itself unresolved: Spring
 * throws NoUniqueBeanDefinitionException rather than picking either one.
 */
@Component
public class ReportingConsumer {

    @Autowired
    private ReportingService reportingService;
}
