package checks.spring.s9352;

import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

// Scenario: only one candidate exists, no issue expected.
// The only bean of type ResourceLoaderAware in this package, used only by SingleCandidateConsumer. A distinct
// interface from the other scenarios in this package, so that a whole-module scan does not merge candidate pools
// across scenarios.
@Component
public class ResourceLoaderComponent implements ResourceLoaderAware {

  @Override
  public void setResourceLoader(ResourceLoader resourceLoader) {
    // not needed for test
  }
}
