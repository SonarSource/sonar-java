package com.example.app;

import com.example.common.DiscountService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * CASE: ambiguity resolved via @Qualifier on a constructor parameter, cross-module.
 * StandardDiscountService lives in module-a, PremiumDiscountService in module-b.
 */
@Component
public class DiscountConsumer {

    private final DiscountService discountService;

    public DiscountConsumer(@Qualifier("premiumDiscountService") DiscountService discountService) {
        this.discountService = discountService;
    }
}
