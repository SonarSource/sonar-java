package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Service;

// Three beans of type ApplicationEventPublisherAware exist. Even with no profile active, the two unprofiled
// candidates are ambiguous and not primary: issue expected.
@Service
public class EventPublisherConsumer {

  @Autowired
  private ApplicationEventPublisherAware contextAware;
}
