package checks.spring.s9352;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Component;

// See PlainEventPublisherComponentA for context.
@Component
public class PlainEventPublisherComponentB implements ApplicationEventPublisherAware {

  @Override
  public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
    // not needed for test
  }
}
