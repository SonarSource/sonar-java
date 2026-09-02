package com.example.app;

import com.example.common.PricingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// CASE: ambiguity resolved via @Primary, same module.
@Component
public class PricingConsumer {

    @Autowired
    private PricingService pricingService;
}
