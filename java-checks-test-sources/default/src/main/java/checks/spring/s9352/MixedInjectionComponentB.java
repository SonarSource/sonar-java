package checks.spring.s9352;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.stereotype.Component;
import org.springframework.util.StringValueResolver;

// See MixedInjectionComponentA for context.
@Component
public class MixedInjectionComponentB implements EmbeddedValueResolverAware {

  @Override
  public void setEmbeddedValueResolver(StringValueResolver resolver) {
    // not needed for test
  }
}
