package com.example.moduleb;

import com.example.common.PaymentGateway;
import org.springframework.stereotype.Component;

@Component
public class DigitalWalletPaymentGateway implements PaymentGateway {
    @Override
    public void charge(double amount) {
        System.out.println("Charging digital wallet: " + amount);
    }
}
