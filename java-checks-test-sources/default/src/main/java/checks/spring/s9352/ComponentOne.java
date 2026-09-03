package checks.spring.s9352;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

// Two candidates of type ApplicationContextAware (this class and ComponentTwo), neither @Primary, reused
// across several scenarios in this package: UnresolvedConsumer (unqualified, ambiguous), NameMatchConsumer
// (resolved by field name), QualifierConsumer (resolved by @Qualifier), and MixedInjectionConsumer (one
// resolved, one unresolved injection point of this type).
@Component
public class ComponentOne implements ApplicationContextAware {

  @Override
  public void setApplicationContext(ApplicationContext ctx) {
    // not needed for test
  }
}
