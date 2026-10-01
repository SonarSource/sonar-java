package com.example.modulea;

import com.example.common.PaymentGateway;
import org.springframework.stereotype.Component;

@Component
public class CreditCardPaymentGateway implements PaymentGateway {
    @Override
    public void charge(double amount) {
        System.out.println("Charging credit card: " + amount);
    }
}
