package checks.spring.s9352;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.stereotype.Component;
import org.springframework.util.StringValueResolver;

// Scenario: one injection point resolves this type by qualifier, but a second, unrelated injection point of the
// same type on the same bean does not: the dependency remains ambiguous for that second point, issue expected.
// Two candidates of type EmbeddedValueResolverAware (this class and MixedInjectionComponentB), used only by
// MixedInjectionConsumer in this scenario. A distinct interface from the other scenarios in this package, so
// that a whole-module scan does not merge candidate pools across scenarios.
@Component
public class MixedInjectionComponentA implements EmbeddedValueResolverAware {

  @Override
  public void setEmbeddedValueResolver(StringValueResolver resolver) {
    // not needed for test
  }
}
