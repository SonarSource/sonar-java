package checks;

abstract class DisallowedThreadGroupCheck {
  void method_removed_in_java_21(ThreadGroup tg) { // Noncompliant
    tg.allowThreadSuspension(true); // Compliant, Note: the "allowThreadSuspension" has been removed in Java 21
    tg.resume(); // Compliant, Note: the "resume" has been removed in Java 23
    tg.stop(); // Compliant, Note: the "stop" has been removed in Java 23
    tg.suspend(); // Compliant, Note: the "suspend" has been removed in Java 23
  }
}
