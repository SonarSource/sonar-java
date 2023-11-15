package checks.spring;

import jakarta.inject.Inject;
import org.springframework.stereotype.Component;

@Component
public class FieldDependencyInjectionCheckJakartaSample {
  @Inject // Noncompliant
  private String injected;

  @Inject // Compliant
  public FieldDependencyInjectionCheckJakartaSample(String injected) {
  }

  @Inject // Compliant
  public void setInjected(String injected) {
    this.injected = injected;
  }
}
