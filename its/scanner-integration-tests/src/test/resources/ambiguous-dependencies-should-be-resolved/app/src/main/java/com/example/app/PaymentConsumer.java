package com.example.app;

import com.example.common.PaymentGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * CASE: unresolved ambiguity, cross-module.
 * CreditCardPaymentGateway lives in module-a, DigitalWalletPaymentGateway in module-b.
 */
@Component
public class PaymentConsumer {

    @Autowired
    private PaymentGateway paymentGateway;
}
