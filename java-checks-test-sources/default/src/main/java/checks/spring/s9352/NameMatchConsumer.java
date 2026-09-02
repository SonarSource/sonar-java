package checks.spring.s9352;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// Two beans of type BeanFactoryAware exist, and this field's name matches the bean name "beanFactoryComponentA"
// exactly: Spring resolves by name, no issue expected.
@Service
public class NameMatchConsumer {

  @Autowired
  private BeanFactoryAware beanFactoryComponentA;
}
