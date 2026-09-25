package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

// Two candidates of type ApplicationContext exist (getPrimary, getSecondary from QualifiedBeanDefinitionConfig),
// and @Qualifier("primaryCtx") matches the @Qualifier on the "getPrimary" bean definition — not its method name:
// disambiguated, no issue expected.
@Service
public class QualifiedBeanDefinitionConsumer {

  @Autowired
  @Qualifier("primaryCtx")
  private ApplicationContext applicationContext;
}
