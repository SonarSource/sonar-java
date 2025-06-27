package checks;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BinaryOperator;
import java.util.stream.Gatherer;
import java.util.stream.Stream;

public class UseOfSequentialForSequentialGathererCheckSample {


  void nonCompliant() {
    Gatherer<Integer, AtomicInteger, Integer> defaultCombiner = Gatherer.of(
      AtomicInteger::new,
      (state, element, downstream) -> downstream.push(element - state.getAndSet(element)),
      Gatherer.defaultCombiner(),  // Noncompliant {{Replace `Gatherer.of(initializer, integrator, combiner, finisher)` with `ofSequential(initializer, integrator, finisher)`}}
    //^^^^^^^^^^^^^^^^^^^^^^^^^^
      Gatherer.defaultFinisher());

    Gatherer<Integer, AtomicInteger, Integer> throwException = Gatherer.of(
      AtomicInteger::new,
      (state, element, downstream) -> downstream.push(element - state.getAndSet(element)),
      (s1, s2) -> {
        throw new IllegalStateException(); // Noncompliant
    //  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
      },
      Gatherer.defaultFinisher());

  }


  void compliant(boolean b) {
    Gatherer<Integer, AtomicInteger, Integer> noBlock = Gatherer.<Integer, AtomicInteger, Integer>of(
      AtomicInteger::new,
      (state, element, downstream) -> true,
      (left, right) -> new AtomicInteger(0),
      (res, _) -> res.get()
    );

    Gatherer<Integer, AtomicInteger, Integer> blockOneStmt = Gatherer.of(
      AtomicInteger::new,
      (state, element, downstream) -> true,
      (left, right) -> {
        return new AtomicInteger(0);
      },
      (res, _) -> res.get()
    );

    Gatherer<Integer, AtomicInteger, Integer> severalBranchAndThrow = Gatherer.<Integer, AtomicInteger, Integer>of(
      AtomicInteger::new,
      (state, element, downstream) -> true,
      (left, right) -> {
        if (b) {
          throw new IllegalStateException();
        }
        return left;
      },
      (res, _) -> res.get()
    );
  }

  void falseNegatives() {
    Gatherer<Integer, AtomicInteger, Integer> twoStmtBody = Gatherer.<Integer, AtomicInteger, Integer>of(
      AtomicInteger::new,
      (state, element, downstream) -> true,
      (s1, s2) -> {
        System.out.println("sequential gatherer");
        throw new IllegalStateException(); // FN
      },
      Gatherer.defaultFinisher());

    Gatherer<Integer, AtomicInteger, Integer> methodRef = Gatherer.of(
      AtomicInteger::new,
      (state, element, downstream) -> true,
      UseOfSequentialForSequentialGathererCheckSample::methodRefNotSupported, // FN
      Gatherer.defaultFinisher());

    Gatherer<Integer, AtomicInteger, Integer> methodInvocation = Gatherer.of(
      AtomicInteger::new,
      (state, element, downstream) -> true,
      noSupported(), // FN
      Gatherer.defaultFinisher());
  }


  static AtomicInteger methodRefNotSupported(AtomicInteger a, AtomicInteger b) {
    throw new IllegalStateException();
  }

  static BinaryOperator<AtomicInteger> noSupported() {
    return (left, right) -> {
      throw new IllegalStateException();
    };
  }
}
