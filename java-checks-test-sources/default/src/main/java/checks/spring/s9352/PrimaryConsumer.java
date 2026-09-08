package checks.spring.s9352;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// Two beans of type BeanNameAware exist, one of them (PrimaryComponent) is @Primary: disambiguated, no issue
// expected.
@Service
public class PrimaryConsumer {

  @Autowired
  private BeanNameAware contextAware;
}
