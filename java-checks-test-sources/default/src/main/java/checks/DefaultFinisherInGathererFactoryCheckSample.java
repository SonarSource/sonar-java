package checks;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Gatherer;

public class DefaultFinisherInGathererFactoryCheckSample {
  Gatherer<Integer, AtomicInteger, Integer> noncompliantGatherer = Gatherer.ofSequential(
    () -> new AtomicInteger(-1),
    (state, number, downstream) -> {
      if (state.get() < 0) {
        state.set(number);
        return true;
      }
      return downstream.push(number - state.get());
    },
    Gatherer.defaultFinisher()); // noncompliant {{Remove the default finisher from this Gatherer factory.}}

  Gatherer<Integer, AtomicInteger, Integer> noncompliantGatherWithLambdaFinisher = Gatherer.ofSequential(
    () -> new AtomicInteger(-1),
    (state, number, downstream) -> {
      if (state.get() < 0) {
        state.set(number);
        return true;
      }
      return downstream.push(number - state.get());
    },
    (state, number) -> { }); // noncompliant {{Remove the default finisher from this Gatherer factory.}}

  Gatherer<Integer, AtomicInteger, Integer> compliantGatherer = Gatherer.ofSequential(
    () -> new AtomicInteger(-1),
    (state, number, downstream) -> {
      if (state.get() < 0) {
        state.set(number);
        return true;
      }
      return downstream.push(number - state.get());
    });
}
