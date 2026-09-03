package checks.spring.s9352;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// Three beans of type BeanClassLoaderAware exist; ProfiledPrimaryClassLoaderComponent is the unique @Primary
// on the raw candidate set (profiled or not), so no issue is expected.
@Service
public class ClassLoaderConsumer {

  @Autowired
  private BeanClassLoaderAware contextAware;
}
