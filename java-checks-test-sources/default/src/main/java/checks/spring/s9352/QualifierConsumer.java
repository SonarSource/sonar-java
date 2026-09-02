package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.EnvironmentAware;
import org.springframework.stereotype.Service;

// Two beans of type EnvironmentAware exist, and @Qualifier matches the bean name "environmentComponentB" exactly:
// disambiguated, no issue expected.
@Service
public class QualifierConsumer {

  @Autowired
  @Qualifier("environmentComponentB")
  private EnvironmentAware contextAware;
}
