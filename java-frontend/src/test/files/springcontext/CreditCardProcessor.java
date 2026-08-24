package checks.spring.context;

import org.springframework.stereotype.Component;

@Component("creditCard")
class CreditCardProcessor implements PaymentProcessor {

  @Override
  public void process() {
    // not needed for test
  }
}
