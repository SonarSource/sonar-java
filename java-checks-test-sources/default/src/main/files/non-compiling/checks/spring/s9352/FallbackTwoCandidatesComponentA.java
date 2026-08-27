package checks.spring.s9352;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.stereotype.Component;

// Scenario: @Fallback does not resolve ambiguity when at least two other (non-fallback) candidates remain.
@Component
public class FallbackTwoCandidatesComponentA implements MessageSourceAware {

  @Override
  public void setMessageSource(MessageSource messageSource) {
    // not needed for test
  }
}
