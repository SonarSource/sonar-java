package com.example.modulea;

import com.example.common.ShippingCarrier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class PrimaryCarrier implements ShippingCarrier {
    @Override
    public String track(String trackingId) {
        return "primary:" + trackingId;
    }
}
