package checks;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Gatherer;
import java.util.stream.Stream;

public class ForStatelessGatherersOmitInitializerCheckSample {
  void nonCompliantSingleReturnNull() {
    Gatherer<Integer, ?, Integer> trivial = Gatherer.<Integer, Void, Integer>ofSequential(
      () -> null, // Noncompliant  {{Replace of `Gatherer.ofSequential(initializer, integrator)` with `Gatherer.ofSequential(integrator)`}}
      //    ^^^^
      (_, element, downstream) -> downstream.push(element)
    );
    Stream.of(1, 2, 3).gather(trivial).forEach(System.out::println);

    Gatherer<Integer, ?, Integer> lambdaWithBody = Gatherer.<Integer, Void, Integer>ofSequential(
      () -> {
        System.out.println("initializer");
        return null; // Noncompliant
      //^^^^^^^^^^^^
      },
      (_, element, downstream) -> downstream.push(element)
    );
    Stream.of(1, 2, 3).gather(lambdaWithBody).forEach(System.out::println);
  }


  void nonCompliantSingleReturnOptional() {
    Gatherer<Integer, ?, Integer> trivial = Gatherer.<Integer, Optional<Object>, Integer>ofSequential(
      () -> Optional.empty(), // Noncompliant
      //    ^^^^^^^^^^^^^^^^
      (_, element, downstream) -> downstream.push(element)
    );
    Stream.of(1, 2, 3).gather(trivial).forEach(System.out::println);

    Gatherer<Integer, ?, Integer> finisher = Gatherer.<Integer, Optional<Long>, Integer>ofSequential(
      () -> Optional.empty(), // Noncompliant {{Replace of `Gatherer.ofSequential(initializer, integrator, finisher)` with `Gatherer.ofSequential(integrator, finisher)`}}
      (_, element, downstream) -> downstream.push(element),
      (_, downstream) -> System.out.println("finisher")
    );
    Stream.of(1, 2, 3).gather(finisher).forEach(System.out::println);
  }


  void compliantStateLess() {
    Gatherer<Integer, ?, Integer> trivial = Gatherer.<Integer, Integer>ofSequential(
      (_, element, downstream) -> downstream.push(element)
    );
    Stream.of(1, 2, 3).gather(trivial).forEach(System.out::println);

    Gatherer<Integer, ?, Integer> finisher = Gatherer.<Integer, Integer>ofSequential(
      (_, element, downstream) -> downstream.push(element),
      (_, downstream) -> System.out.println("finisher")
    );
    Stream.of(1, 2, 3).gather(finisher).forEach(System.out::println);
  }


  void compliantStateful() {
    Gatherer<Integer, ?, Integer> object = Gatherer.<Integer, Object, Integer>ofSequential(
      Object::new,
      (_, element, downstream) -> downstream.push(element)
    );
    Stream.of(1, 2, 3).gather(object).forEach(System.out::println);

    Gatherer<Integer, ?, Integer> option = Gatherer.<Integer, Optional<String>, Integer>ofSequential(
      () -> Optional.of("state"),
      (_, element, downstream) -> downstream.push(element)
    );
    Stream.of(1, 2, 3).gather(option).forEach(System.out::println);

    Gatherer<Integer, List<Integer>, Integer> finisher = Gatherer.<Integer, List<Integer>, Integer>ofSequential(
      LinkedList::new,
      (_, element, downstream) -> downstream.push(element),
      (_, downstream) -> System.out.println("finisher")
    );
    Stream.of(1, 2, 3).gather(finisher).forEach(System.out::println);
  }


  void noncompliantMultipleReturns(boolean condition) {
    Gatherer<Integer, ?, Integer> g = Gatherer.<Integer, Void, Integer>ofSequential(
      () -> {
        if (condition) {
          return null; // Noncompliant {{Replace of `Gatherer.ofSequential(initializer, integrator)` with `Gatherer.ofSequential(integrator)`}}
      //  ^^^^^^^^^^^^
        }
        return null;
      //^^^^^^^^^^^^< {{}}
      },
      (_, element, downstream) -> downstream.push(element)
    );
    Stream.of(1, 2, 3).gather(g).forEach(System.out::println);
  }

  void compliantMultipleReturns(boolean condition) {
    Gatherer<Integer, ?, Integer> g = Gatherer.<Integer, Object, Integer>ofSequential(
      () -> {
        if (condition) {
          return new Object();
        }
        return null;
      },
      (_, element, downstream) -> downstream.push(element)
    );
    Stream.of(1, 2, 3).gather(g).forEach(System.out::println);
  }

}
