package checks.spring;

import io.micronaut.function.aws.MicronautRequestHandler;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FieldDependencyInjectionCheckSample {
  @Autowired // Noncompliant {{Remove this field injection and use constructor injection instead.}}
//^^^^^^^^^^
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

  private class Handler extends MicronautRequestHandler<Object, Object> {
    @Inject // Compliant : MicronautRequestHandler requires a no-arg constructor for AWS
    private String injected;

    public Handler() {
      // Required no-arg constructor for AWS
    }

    @Override
    public Object execute(Object input) {
      return null;
    }
  }
}
