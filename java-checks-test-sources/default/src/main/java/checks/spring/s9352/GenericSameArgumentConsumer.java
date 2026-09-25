package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

interface Bin<T> {
}

class Widget {
}

@Component
class CachingWidgetBin implements Bin<Widget> {
}

@Component
class JdbcWidgetBin implements Bin<Widget> {
}

// Both beans implement Bin with the same type argument, so resolving generics does not disambiguate them and
// Spring fails at startup. The issue must still be reported.
@Service
public class GenericSameArgumentConsumer {

  @Autowired
  private Bin<Widget> bin;
}
