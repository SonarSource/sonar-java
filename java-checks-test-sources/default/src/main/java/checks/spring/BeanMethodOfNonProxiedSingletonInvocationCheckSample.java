package checks.spring;

import java.util.Random;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import static java.lang.System.lineSeparator;

public class BeanMethodOfNonProxiedSingletonInvocationCheckSample {
  static class SimpleBean {
    // ...
  }

  @Scope("Prototype")
  static class PrototypeBean {
    public PrototypeBean(SimpleBean simpleBean) {
      // ...
    }
  }

  static class NamedBean {
    private String name;

    public NamedBean(String name) {
      this.name = name;
    }
  }

  private static String getAString() {
    return (new Random(42)).nextBoolean() ? "Nothing" : "Something";
  }

  @Configuration(proxyBeanMethods = false)
  static class NonCompliantConfiguration {
    @Bean
    public SimpleBean singletonBean() {
      return new SimpleBean();
    }

    @Bean
    public PrototypeBean prototypeBean() {
      return new PrototypeBean(singletonBean()); // Noncompliant, the singletonBean is created every time a prototypeBean is created
    }

    @Bean
    public NamedBean namedBean() {
      return new NamedBean(lineSeparator());
    }

    public NamedBean anotherNamedBean() {
      return new NamedBean(getAString());
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class CompliantConfiguration {
    @Bean
    public SimpleBean singletonBean() {
      return new SimpleBean();
    }

    @Bean
    public PrototypeBean prototypeBean(SimpleBean simpleBean) { // Compliant, the singletonBean is injected in the context and used by every prototypeBean
      return new PrototypeBean(simpleBean);
    }

    @Bean
    public NamedBean namedBean() {
      return new NamedBean(lineSeparator());
    }
  }

  @Configuration(proxyBeanMethods = true)
  static class ProxyBeanMethodsEnabledExplicitly {
    @Bean
    public SimpleBean singletonBean() {
      return new SimpleBean();
    }

    @Bean
    public PrototypeBean prototypeBean() {
      return new PrototypeBean(singletonBean()); // Compliant, call will be proxied and the singleton instance will be returned
    }

    @Bean
    public NamedBean namedBean() {
      return new NamedBean(lineSeparator());
    }
  }

  @Configuration(value = "nothingToSeeHere")
  static class ProxyBeanMethodsEnabledImplicitly {
    @Bean
    public SimpleBean singletonBean() {
      return new SimpleBean();
    }

    @Bean
    public PrototypeBean prototypeBean() {
      return new PrototypeBean(singletonBean()); // Compliant, call will be proxied and the singleton instance will be returned
    }

    @Bean
    public NamedBean namedBean() {
      return new NamedBean(lineSeparator());
    }
  }
}
