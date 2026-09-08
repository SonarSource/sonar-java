package checks.spring.s9352;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// Scenario: a @Primary candidate that also happens to be profiled must not be discarded before the primary
// check runs — it is already the unique primary on the raw candidate set, so no issue is expected regardless
// of its profile. Three candidates of type BeanClassLoaderAware (this class, PlainClassLoaderComponentA,
// PlainClassLoaderComponentB), used only by ClassLoaderConsumer in this scenario. A distinct interface from
// the other scenarios in this package, so that a whole-module scan does not merge candidate pools across
// scenarios.
@Primary
@Profile("prod")
@Component
public class ProfiledPrimaryClassLoaderComponent implements BeanClassLoaderAware {

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    // not needed for test
  }
}
