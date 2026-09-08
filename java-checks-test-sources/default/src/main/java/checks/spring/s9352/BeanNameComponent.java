package checks.spring.s9352;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.stereotype.Component;

// Scenario: @Primary disambiguates, no issue expected.
// Two candidates of type BeanNameAware (this class and PrimaryComponent), the latter @Primary, used only by
// PrimaryConsumer in this scenario. A distinct interface from the other scenarios in this package, so that a
// whole-module scan does not merge candidate pools across scenarios.
@Component
public class BeanNameComponent implements BeanNameAware {

  @Override
  public void setBeanName(String name) {
    // not needed for test
  }
}
