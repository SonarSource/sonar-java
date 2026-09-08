package com.example.modulea;

import com.example.common.DiscountService;
import org.springframework.stereotype.Component;

@Component
public class StandardDiscountService implements DiscountService {
    @Override
    public double applyDiscount(double amount) {
        return amount * 0.95;
    }
}
