package checks.spring.s9352;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.stereotype.Component;

// Scenario: @Fallback does not resolve ambiguity when at least two other (non-fallback) candidates remain.
// Three candidates of type MessageSourceAware exist: this class, FallbackTwoCandidatesComponentB (both regular),
// and FallbackTwoCandidatesFallbackComponent (@Fallback). Spring ignores the fallback candidate only when a
// single non-fallback candidate remains; here two non-fallback candidates still compete, so the dependency
// remains ambiguous. Not yet handled by AmbiguousDependencyCheck (see FallbackRegularComponent for context on
// why @Fallback support is pending), so this currently (incorrectly) raises no issue; it should once handled.
// A distinct interface from the other scenarios in this package, so that a whole-module scan does not merge
// candidate pools across scenarios.
@Component
public class FallbackTwoCandidatesComponentA implements MessageSourceAware {

  @Override
  public void setMessageSource(MessageSource messageSource) {
    // not needed for test
  }
}
