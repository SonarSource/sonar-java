package checks.spring.s9352;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.stereotype.Component;

@Component
public class PlainClassLoaderComponentB implements BeanClassLoaderAware {

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
  }
}
