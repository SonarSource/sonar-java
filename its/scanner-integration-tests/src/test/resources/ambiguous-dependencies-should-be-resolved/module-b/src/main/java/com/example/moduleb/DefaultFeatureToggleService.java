package com.example.moduleb;

import com.example.common.FeatureToggleService;
import org.springframework.stereotype.Component;

@Component
public class DefaultFeatureToggleService implements FeatureToggleService {
    @Override
    public boolean isEnabled(String feature) {
        return false;
    }
}
