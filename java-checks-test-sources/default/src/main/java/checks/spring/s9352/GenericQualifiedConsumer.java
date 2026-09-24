package checks.spring.s9352;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

// Both beans of GenericQualifiedBeanConfig match this type, and the @Qualifier selects one. No issue expected.
@Service
public class GenericQualifiedConsumer {

  @Autowired
  @Qualifier("byId")
  private Map<Integer, ApplicationContextAware> index;
}
