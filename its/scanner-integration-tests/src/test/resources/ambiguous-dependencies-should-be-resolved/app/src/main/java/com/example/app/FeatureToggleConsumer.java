package com.example.app;

import com.example.common.FeatureToggleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * CASE: ambiguity resolved via @Profile exclusion, cross-module.
 * ProdFeatureToggleService (@Profile("prod")) lives in module-a, DefaultFeatureToggleService in module-b.
 * Excluding the profiled candidate leaves DefaultFeatureToggleService as the only one.
 */
@Component
public class FeatureToggleConsumer {

    @Autowired
    private FeatureToggleService featureToggleService;
}
