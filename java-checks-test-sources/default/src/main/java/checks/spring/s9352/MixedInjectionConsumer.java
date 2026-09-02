package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.stereotype.Service;

// See MixedInjectionComponentA for context: "resolved" is fine, but "unresolved" is genuinely ambiguous and
// must still be reported, even though both injection points share the same required type.
@Service
public class MixedInjectionConsumer {

  @Autowired
  @Qualifier("mixedInjectionComponentA")
  private EmbeddedValueResolverAware resolved;

  @Autowired
  private EmbeddedValueResolverAware unresolved;
}
