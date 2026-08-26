package checks.spring.s9352;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

// @Fallback (available since Spring 6.2, not present in the spring-context version on this module's classpath,
// hence non-compiling) is not yet handled by AmbiguousDependencyCheck: Spring would resolve this dependency
// unambiguously by ignoring the fallback candidate, but the check does not know that yet. Kept here for when
// @Fallback support is added. See FallbackComponent and FallbackConsumer.
@Component
class FallbackRegularComponent implements ApplicationContextAware {

  @Override
  public void setApplicationContext(ApplicationContext ctx) {
    // not needed for test
  }
}
