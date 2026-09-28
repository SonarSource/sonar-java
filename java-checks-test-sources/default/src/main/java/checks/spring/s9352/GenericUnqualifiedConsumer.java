package checks.spring.s9352;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

// Same two beans, but nothing here selects between them: the field name matches neither bean name nor
// qualifier, so the dependency really is ambiguous and an issue is expected.
@Service
public class GenericUnqualifiedConsumer {

  @Autowired
  private Map<Integer, ApplicationContextAware> index;
}
