package checks.spring.s9352;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

// Two beans of type ApplicationContextAware exist (ComponentOne, ComponentTwo), but Spring injects all of them
// into each of these fields instead of resolving a single one, so none of these injection points can be
// ambiguous. No issue expected.
@Service
public class CollectionInjectionConsumer {

  @Autowired
  private List<ApplicationContextAware> listOfContextAwares;

  @Autowired
  private Set<ApplicationContextAware> setOfContextAwares;

  @Autowired
  private Collection<ApplicationContextAware> collectionOfContextAwares;

  @Autowired
  private ApplicationContextAware[] arrayOfContextAwares;
}
