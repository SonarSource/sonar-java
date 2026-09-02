package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

// Two beans of type ApplicationContextAware exist (ComponentOne, ComponentTwo), neither is @Primary, and this
// field's name matches neither bean name: ambiguous, issue expected.
@Service
public class UnresolvedConsumer {

  @Autowired
  private ApplicationContextAware contextAware;
}
