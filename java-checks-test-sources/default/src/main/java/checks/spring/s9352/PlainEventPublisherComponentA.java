package checks.spring.s9352;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Component;

// Scenario: excluding a profiled candidate still leaves two ambiguous, unprimaried candidates — issue expected.
// Three candidates of type ApplicationEventPublisherAware (this class, PlainEventPublisherComponentB,
// ProfiledEventPublisherComponent), used only by EventPublisherConsumer in this scenario. A distinct interface
// from the other scenarios in this package, so that a whole-module scan does not merge candidate pools across
// scenarios.
@Component
public class PlainEventPublisherComponentA implements ApplicationEventPublisherAware {

  @Override
  public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
    // not needed for test
  }
}
