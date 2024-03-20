package checks;

abstract class DisallowedThreadGroupCheckSample {
  void method_removed_in_java_21(ThreadGroup tg) { // Noncompliant
    tg.allowThreadSuspension(true); // Compliant, Note: the "allowThreadSuspension" has been removed in Java 21
  }
}
