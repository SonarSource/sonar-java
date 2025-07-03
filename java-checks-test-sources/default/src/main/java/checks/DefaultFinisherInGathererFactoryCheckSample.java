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
    }, Gatherer.defaultFinisher()); // noncompliant {{Remove the default finisher from this Gatherer factory.}} [[quickfixes=qf1]]
//     ^^^^^^^^^^^^^^^^^^^^^^^^^^
// fix@qf1 {{Remove finisher argument.}}
// edit@qf1 [[sc=6;ec=34]] {{}}


  Gatherer<Integer, AtomicInteger, Integer> noncompliantGatherWithLambdaFinisher = Gatherer.ofSequential(
    () -> new AtomicInteger(-1),
    (state, number, downstream) -> {
      if (state.get() < 0) {
        state.set(number);
        return true;
      }
      return downstream.push(number - state.get());
    }, (state, number) -> { }); // noncompliant {{Remove the default finisher from this Gatherer factory.}} [[quickfixes=qf2]]
//     ^^^^^^^^^^^^^^^^^^^^^^
// fix@qf2 {{Remove finisher argument.}}
// edit@qf2 [[sc=6;ec=30]] {{}}

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
