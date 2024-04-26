package checks;

import java.util.Optional;
import java.util.Random;
import java.util.stream.*;

class PreferStreamAnyMatch {

  void test(Random r) {
    IntStream.range(0, 10).filter(i -> true).findFirst().isPresent(); // Noncompliant {{Replace this "filter().findFirst().isPresent()" chain with "anyMatch()".}}
//                         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    IntStream.range(0, 10).filter(i -> true).findAny().isPresent(); // Noncompliant {{Replace this "filter().findAny().isPresent()" chain with "anyMatch()".}}
//                         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    LongStream.range(0, 10).filter(i -> true).findFirst().isPresent(); // Noncompliant
    LongStream.range(0, 10).filter(i -> true).findAny().isPresent(); // Noncompliant
    r.doubles(10).filter(i -> true).findFirst().isPresent(); // Noncompliant
    r.doubles(10).filter(i -> true).findAny().isPresent(); // Noncompliant
    Stream.of("a", "b", "c").filter(s -> s.startsWith("a")).findFirst().isPresent(); // Noncompliant
    Stream.of("a", "b", "c").filter(s -> s.startsWith("a")).findAny().isPresent(); // Noncompliant

    Stream.of("a", "b", "c").findAny().isPresent(); // Compliant
  }

  void anymatch(Stream<Object> stream) {
    boolean match = !stream.filter(o -> o instanceof PreferStreamAnyMatch).filter(o -> o != null).anyMatch(o -> true); // Noncompliant {{Replace this negation and "anyMatch()" with "noneMatch()".}}
    match = !stream.anyMatch(o -> !true); // Noncompliant {{Replace this double negation with "allMatch()" and positive predicate.}}
    match = stream.map(o -> o.equals("")).anyMatch(Boolean::booleanValue); // Noncompliant {{Use mapper from "map()" directly as predicate in "anyMatch()".}}
//                                        ^^^^^^^^

    stream.anyMatch(o -> o != null);
  }

  void coverage() {
    Optional<Object> optional = Optional.empty();
    optional.isPresent();
  }

  abstract class MoreCoverage implements Stream {
    void test() {
      findAny().isPresent();
    }
  }
}
