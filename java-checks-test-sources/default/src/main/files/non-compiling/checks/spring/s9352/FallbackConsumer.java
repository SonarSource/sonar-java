package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

// See FallbackRegularComponent for context.
@Service
class FallbackConsumer {

  @Autowired
  private ApplicationContextAware contextAware;
}
