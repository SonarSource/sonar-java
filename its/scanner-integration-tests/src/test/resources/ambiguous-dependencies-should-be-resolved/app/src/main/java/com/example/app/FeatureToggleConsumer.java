package com.example.app;

import com.example.common.FeatureToggleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * CASE: ambiguity arising only under an active profile, cross-module.
 * ProdFeatureToggleService (@Profile("prod")) lives in module-a, DefaultFeatureToggleService in module-b.
 * When the "prod" profile is active both are candidates and nothing disambiguates them.
 */
@Component
public class FeatureToggleConsumer {

    @Autowired
    private FeatureToggleService featureToggleService;
}
