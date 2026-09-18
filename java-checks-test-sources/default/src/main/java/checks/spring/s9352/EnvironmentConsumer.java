package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.stereotype.Service;

@Service
public class EnvironmentConsumer {

  @Autowired
  private EnvironmentAware environmentAware;
}
