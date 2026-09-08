package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Service;

// Three beans of type ApplicationEventPublisherAware exist. Excluding the profiled candidate
// (ProfiledEventPublisherComponent) still leaves two ambiguous, unprimaried candidates: issue expected.
@Service
public class EventPublisherConsumer {

  @Autowired
  private ApplicationEventPublisherAware contextAware;
}
