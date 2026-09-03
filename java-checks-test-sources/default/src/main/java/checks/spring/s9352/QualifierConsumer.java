package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

// Two beans of type ApplicationContextAware exist (ComponentOne, ComponentTwo), and @Qualifier matches the
// bean name "componentTwo" exactly: disambiguated, no issue expected.
@Service
public class QualifierConsumer {

  @Autowired
  @Qualifier("componentTwo")
  private ApplicationContextAware contextAware;
}
