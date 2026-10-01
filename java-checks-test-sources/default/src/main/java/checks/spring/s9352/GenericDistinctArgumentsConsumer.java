package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

interface Crate<T> {
}

class Apple {
}

class Pear {
}

@Component
class AppleCrate implements Crate<Apple> {
}

@Component
class PearCrate implements Crate<Pear> {
}

// Two beans implement Crate, but with different type arguments. Spring resolves the full generic type and
// matches only appleCrate, so this injection point is not ambiguous. No issue expected.
@Service
public class GenericDistinctArgumentsConsumer {

  @Autowired
  private Crate<Apple> crate;
}
