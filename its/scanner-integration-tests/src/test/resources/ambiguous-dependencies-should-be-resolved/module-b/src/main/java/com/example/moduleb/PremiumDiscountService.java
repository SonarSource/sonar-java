package com.example.moduleb;

import com.example.common.DiscountService;
import org.springframework.stereotype.Component;

@Component
public class PremiumDiscountService implements DiscountService {
    @Override
    public double applyDiscount(double amount) {
        return amount * 0.8;
    }
}
