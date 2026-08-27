package checks.spring.s9352;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

// "main" matches QualifierOnBeanComponentA's own declared @Qualifier value, not its bean name: still resolved,
// no issue expected. See QualifierOnBeanComponentA for context.
@Service
public class QualifierOnBeanConsumer {

  @Autowired
  @Qualifier("main")
  private BeanClassLoaderAware contextAware;
}
