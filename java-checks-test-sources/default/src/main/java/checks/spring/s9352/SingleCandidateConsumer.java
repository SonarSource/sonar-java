package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.stereotype.Service;

// Only one bean of type ResourceLoaderAware is registered in this scenario: no ambiguity, no issue expected.
@Service
public class SingleCandidateConsumer {

  @Autowired
  private ResourceLoaderAware contextAware;
}
