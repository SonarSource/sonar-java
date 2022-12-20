package checks.unused;

public class UnusedPrivateMethod {

  private void init(@Observes Object object) {} // Compliant
  private void init2(@UnknownAnnotation Object object) {} // Noncompliant
  
}
