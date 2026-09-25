package checks.spring.s9352;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

// Two beans of type ApplicationContextAware exist (ComponentOne, ComponentTwo), but a map keyed by bean name
// receives all of them, so this injection point cannot be ambiguous. No issue expected.
@Service
public class MapInjectionConsumer {

  @Autowired
  private Map<String, ApplicationContextAware> contextAwaresByName;
}
