package checks.spring.s9352;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// See TwoPrimaryComponentA for context.
@Service
public class TwoPrimaryConsumer {

  @Autowired
  private DisposableBean contextAware;
}
