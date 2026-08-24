package checks.spring.context;

import org.springframework.stereotype.Component;

@Component("paypal")
class PayPalProcessor implements PaymentProcessor {

  @Override
  public void process() {
    // not needed for test
  }
}
