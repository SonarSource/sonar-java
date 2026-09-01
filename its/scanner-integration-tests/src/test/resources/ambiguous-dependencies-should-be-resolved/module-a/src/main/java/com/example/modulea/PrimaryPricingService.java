package com.example.modulea;

import com.example.common.PricingService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class PrimaryPricingService implements PricingService {
    @Override
    public double getPrice() {
        return 9.99;
    }
}
