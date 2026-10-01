package com.example.app;

import com.example.common.ShippingCarrier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * CASE: ambiguity resolved via @Primary, cross-module.
 * PrimaryCarrier (@Primary) lives in module-a, SecondaryCarrier in module-b.
 */
@Component
public class CarrierConsumer {

    @Autowired
    private ShippingCarrier shippingCarrier;
}
