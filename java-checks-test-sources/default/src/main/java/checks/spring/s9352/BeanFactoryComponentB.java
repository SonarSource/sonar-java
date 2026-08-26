package checks.spring.s9352;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.stereotype.Component;

// See BeanFactoryComponentA for context.
@Component
public class BeanFactoryComponentB implements BeanFactoryAware {

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    // not needed for test
  }
}
