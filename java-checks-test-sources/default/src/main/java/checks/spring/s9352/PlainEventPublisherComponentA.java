package checks.spring.s9352;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Component;

@Component
public class PlainEventPublisherComponentA implements ApplicationEventPublisherAware {

  @Override
  public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
  }
}
