package checks.spring;

import jakarta.inject.Inject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

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

@Service
class SpringServiceClassJakarta {
  @Autowired // Noncompliant
  private String autowired;

  @javax.inject.Inject // Noncompliant
  private String injected;
}

class NormalClassJakarta {
  @Autowired // Compliant
  private String autowired;

  @javax.inject.Inject // Compliant
  private String injected;
}
