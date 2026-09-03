package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

// Two beans of type ApplicationContextAware exist (ComponentOne, ComponentTwo), and this field's name matches
// the bean name "componentOne" exactly: Spring resolves by name, no issue expected.
@Service
public class NameMatchConsumer {

  @Autowired
  private ApplicationContextAware componentOne;
}
