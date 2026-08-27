package checks.spring.s9352;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

// Scenario: @Qualifier declared on the bean itself (not on an injection point) resolves ambiguity, no issue
// expected. Two candidates of type BeanClassLoaderAware (this class and QualifierOnBeanComponentB), used only
// by QualifierOnBeanConsumer in this scenario. A distinct interface from the other scenarios in this package,
// so that a whole-module scan does not merge candidate pools across scenarios.
@Qualifier("main")
@Component
public class QualifierOnBeanComponentA implements BeanClassLoaderAware {

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    // not needed for test
  }
}
