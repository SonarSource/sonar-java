package checks.spring.s9352;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.stereotype.Component;

// See FallbackTwoCandidatesComponentA for context.
@org.springframework.context.annotation.Fallback
@Component
public class FallbackTwoCandidatesFallbackComponent implements MessageSourceAware {

  @Override
  public void setMessageSource(MessageSource messageSource) {
    // not needed for test
  }
}
