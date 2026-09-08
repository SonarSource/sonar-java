package com.example.common;

public interface FeatureToggleService {
    boolean isEnabled(String feature);
}
