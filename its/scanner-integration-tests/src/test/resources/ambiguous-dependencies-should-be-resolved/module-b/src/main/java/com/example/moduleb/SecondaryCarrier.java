package com.example.moduleb;

import com.example.common.ShippingCarrier;
import org.springframework.stereotype.Component;

@Component
public class SecondaryCarrier implements ShippingCarrier {
    @Override
    public String track(String trackingId) {
        return "secondary:" + trackingId;
    }
}
