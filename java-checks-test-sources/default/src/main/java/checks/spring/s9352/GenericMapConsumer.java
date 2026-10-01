package checks.spring.s9352;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

// Only contextAwaresById has this map's value type, so the dependency is not ambiguous. No issue expected.
@Service
public class GenericMapConsumer {

  @Autowired
  private Map<Integer, ApplicationContextAware> byId;
}
