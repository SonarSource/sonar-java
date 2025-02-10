package checks.spring;

import org.springframework.context.event.EventListener;

public class EventListenerMethodOneArgCheckSample {
  static class CustomEvent {}

  @EventListener
  void compliantOneArg(CustomEvent customEvent) {
  }

  @EventListener
  String compliantWithReturn(CustomEvent customEvent) {
    return "ok";
  }

  @EventListener
  static void compliantOneArgStatic(CustomEvent customEvent) {
  }

  @EventListener
  void compliantListenerNoArgs() {
  }

  @EventListener
  void twoArgs(CustomEvent customEvent, String anotherParameter) { // Noncompliant
  }

  @EventListener
  void twoArgsStatic(CustomEvent customEvent, String anotherParameter) { // Noncompliant
  }

  @Deprecated
  @EventListener
  void unrelatedAnnotation() {
  }

  @Deprecated
  void unrelatedTwoArgs(int a, int b) {
  }

  void notAnnotated() {}
}
