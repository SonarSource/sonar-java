package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

// Two beans of type InitializingBean exist, both on @Profile("test"): when that profile is active they
// compete with each other, and nothing disambiguates them. Issue expected.
@Service
public class SameProfileConsumer {

  @Autowired
  private InitializingBean initializer;
}
