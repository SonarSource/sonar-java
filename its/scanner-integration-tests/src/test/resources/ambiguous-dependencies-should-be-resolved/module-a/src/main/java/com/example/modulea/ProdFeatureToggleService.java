package com.example.modulea;

import com.example.common.FeatureToggleService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("prod")
@Component
public class ProdFeatureToggleService implements FeatureToggleService {
    @Override
    public boolean isEnabled(String feature) {
        return true;
    }
}
