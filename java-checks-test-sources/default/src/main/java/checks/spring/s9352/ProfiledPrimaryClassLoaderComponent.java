package checks.spring.s9352;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Primary
@Profile("prod")
@Component
public class ProfiledPrimaryClassLoaderComponent implements BeanClassLoaderAware {

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
  }
}
