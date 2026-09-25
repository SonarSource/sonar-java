package checks.spring.s9352;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

// See BeanNameComponent for context.
@Primary
@Component
public class PrimaryComponent implements BeanNameAware {

  @Override
  public void setBeanName(String name) {
    // not needed for test
  }
}
