package checks.spring;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.context.annotation.ScopedProxyMode.TARGET_CLASS;

public class NonSingletonAutowiredInSingletonCheckSampleNonSingletonBeansDefinition {
  private static final String PROTOTYPE_SCOPE = "prototype";

  @Component
  @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = TARGET_CLASS)
  public class RequestBean1 {
  }

  @Scope("prototype")
  public class PrototypeBean1 {
  }

  @Scope(value = "prototype")
  public class PrototypeBean2 {
  }

  @Scope(value = PROTOTYPE_SCOPE, scopeName = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
  public class PrototypeBean3 {
  }
}
