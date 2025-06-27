package checks;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Gatherer;
import java.util.stream.Stream;

public class UseOfSequentialForSequentialGathererCheckSample {


  void nonCompliant() {
    Gatherer<Integer, AtomicInteger, Integer> defaultCombiner = Gatherer.of(
      AtomicInteger::new,
      (state, element, downstream) -> downstream.push(element - state.getAndSet(element)),
      Gatherer.defaultCombiner(),           // Noncompliant
      Gatherer.defaultFinisher());

    Gatherer<Integer, AtomicInteger, Integer> throwException = Gatherer.of(
      AtomicInteger::new,
      (state, element, downstream) -> downstream.push(element - state.getAndSet(element)),
      (s1, s2) -> {
        throw new IllegalStateException();              // Noncompliant
      },
      Gatherer.defaultFinisher());


  }


  void compliant(boolean b) {
    Gatherer<Integer, AtomicInteger, Integer> noThrow = Gatherer.of(
      AtomicInteger::new,
      (state, element, downstream) -> {
        state.addAndGet(element);
        return true;
      },
      (left, right) -> {
        left.addAndGet(right.get());
        return left;
      },
      AtomicInteger::get
    );

    Gatherer<Integer, AtomicInteger, Integer> severalBranchAndThrow = Gatherer.of(
      AtomicInteger::new,
      (state, element, downstream) -> downstream.push(element - state.getAndSet(element)),
      (left, right) -> {
        if (shouldFail) {
          throw new IllegalStateException();
        }
        left.addAndGet(right.get());
        return left;
      },
      AtomicInteger::get
    );
  }
}
