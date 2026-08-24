package checks.spring.context;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
class OrderService {

  private final PaymentProcessor paymentProcessor;

  @Autowired
  OrderService(@Qualifier("paypal") PaymentProcessor paymentProcessor) {
    this.paymentProcessor = paymentProcessor;
  }
}
