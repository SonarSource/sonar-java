package com.example.app;

import com.example.common.PaymentGateway;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * CASE: a bean declared in test scope must not compete with the main-scope ones.
 * <p>
 * Spring Boot's TestTypeExcludeFilter keeps every class nested inside a test class out of component scanning, so
 * testPaymentGateway never joins the context that holds CreditCardPaymentGateway and DigitalWalletPaymentGateway.
 * If it were collected, PaymentConsumer's issue would name three candidates instead of two.
 */
class PaymentConsumerTest {

    @Test
    void paymentGatewayIsInjected() {
        // the body is irrelevant: this class exists to be scanned, not run
    }

    @Configuration
    static class TestPaymentGatewayConfiguration {

        @Bean
        PaymentGateway testPaymentGateway() {
            return amount -> {
                // no-op stub
            };
        }
    }
}
