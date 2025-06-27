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
      (s1, s2) -> {                         // Noncompliant
        throw new IllegalStateException();
      },
      Gatherer.defaultFinisher());


  }



  // Compliant: meaningful non-default combiner, Gatherer.of is appropriate
  void compliant() {
    Gatherer<Integer, AtomicInteger, Integer> combine = Gatherer.of(
      AtomicInteger::new,
      (state, element, downstream) -> {
        state.addAndGet(element);          // accumulate sum
        return true;                       // nothing pushed downstream
      },
      (left, right) -> {                   // proper combiner
        left.addAndGet(right.get());
        return left;
      },
      AtomicInteger::get                   // finisher returns the sum
    );

  }

  // Noncompliant: one branch of the combiner throws, indicating sequential intent
  void nonCompliantBranchThrowsCombiner(boolean shouldFail) {
    Gatherer<Integer, AtomicInteger, Integer> g = Gatherer.of(
      AtomicInteger::new,
      (state, element, downstream) -> downstream.push(element - state.getAndSet(element)),
      (left, right) -> {                  // Noncompliant
        if (shouldFail) {
          throw new IllegalStateException();
        }
        left.addAndGet(right.get());
        return left;
      },
      AtomicInteger::get
    );
    int result = Stream.of(1, 2, 3).gather(g).findFirst().orElse(0);
    System.out.println(result);
  }
}
