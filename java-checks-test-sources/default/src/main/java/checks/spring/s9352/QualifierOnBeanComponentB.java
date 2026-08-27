package checks.spring.s9352;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.stereotype.Component;

// See QualifierOnBeanComponentA for context.
@Component
public class QualifierOnBeanComponentB implements BeanClassLoaderAware {

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    // not needed for test
  }
}
