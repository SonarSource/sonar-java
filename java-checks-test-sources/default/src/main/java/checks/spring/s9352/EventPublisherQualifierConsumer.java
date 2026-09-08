package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Service;

// See PlainEventPublisherComponentA for context. This injection point is explicitly qualified towards the
// profiled candidate: already disambiguated by name/qualifier, no issue expected, even though
// ProfiledEventPublisherComponent is excluded from the uniqueness/@Primary heuristic used for unqualified
// injection points of this type (see EventPublisherConsumer).
@Service
public class EventPublisherQualifierConsumer {

  @Autowired
  @Qualifier("profiledEventPublisherComponent")
  private ApplicationEventPublisherAware contextAware;
}
