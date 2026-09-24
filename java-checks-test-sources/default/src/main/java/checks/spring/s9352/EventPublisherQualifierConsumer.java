package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Service;

// See PlainEventPublisherComponentA for context. This injection point is explicitly qualified towards the
// profiled candidate: it designates a single bean whichever profile is active, so no issue is expected, unlike
// the unqualified injection point of the same type (see EventPublisherConsumer).
@Service
public class EventPublisherQualifierConsumer {

  @Autowired
  @Qualifier("profiledEventPublisherComponent")
  private ApplicationEventPublisherAware contextAware;
}
