package checks.spring;

import javax.annotation.Nullable;
import javax.inject.Inject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FieldDependencyInjectionCheckSample {
  @Autowired // Noncompliant [[sc=3;ec=13]] {{Remove this field injection and use constructor injection instead.}}
  private String autowired;

  @Inject // Noncompliant
  private String injected;

  @Autowired // Noncompliant
  @Nullable
  private String injectedAndNullable;

  @Autowired // Compliant
  public FieldDependencyInjectionCheckSample() {
  }

  @Inject // Compliant
  public FieldDependencyInjectionCheckSample(String injected) {
  }

  @Autowired // Compliant
  public void setAutowired(String autowired) {
    this.autowired = autowired;
  }

  @Inject // Compliant
  public void setInjected(String injected) {
    this.injected = injected;
  }

  private String notInjected;

  @Nullable
  private String annotatedButNotInjected;
}
