package checks.unused;

public class UnusedPrivateMethodWithUknownResolution {

  private void init(@Observes Object object) {} // Compliant
  private void init2(@UnknownAnnotation Object object) {} // Noncompliant
  
}
