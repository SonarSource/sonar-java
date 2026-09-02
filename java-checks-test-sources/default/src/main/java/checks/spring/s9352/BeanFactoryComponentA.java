package checks.spring.s9352;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.stereotype.Component;

// Scenario: field name matches a bean name, no issue expected.
// Two candidates of type BeanFactoryAware (this class and BeanFactoryComponentB), used only by
// NameMatchConsumer in this scenario. A distinct interface from the other scenarios in this package, so that a
// whole-module scan does not merge candidate pools across scenarios.
@Component
public class BeanFactoryComponentA implements BeanFactoryAware {

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    // not needed for test
  }
}
