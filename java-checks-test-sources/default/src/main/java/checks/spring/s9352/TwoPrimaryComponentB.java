package checks.spring.s9352;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

// See TwoPrimaryComponentA for context.
@Primary
@Component
public class TwoPrimaryComponentB implements DisposableBean {

  @Override
  public void destroy() {
    // not needed for test
  }
}
