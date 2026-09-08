package checks.spring.s9352;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

// Scenario: an unprofiled @Primary candidate still disambiguates even though another, profiled candidate is also
// @Primary — no issue expected. Regression test: the primary/unique check must be re-applied after profiled
// candidates are excluded, not decided once on the raw candidate set.
// Three candidates of type MessageSourceAware (this class, ProfiledPrimaryComponent, PlainMessageSourceComponent),
// used only by MessageSourceConsumer in this scenario. A distinct interface from the other scenarios in this
// package, so that a whole-module scan does not merge candidate pools across scenarios.
@Primary
@Component
public class UnprofiledPrimaryComponent implements MessageSourceAware {

  @Override
  public void setMessageSource(MessageSource messageSource) {
    // not needed for test
  }
}
