package com.example.modulea;

import com.example.common.PricingService;
import org.springframework.stereotype.Component;

@Component
public class StandardPricingService implements PricingService {
    @Override
    public double getPrice() {
        return 14.99;
    }
}
