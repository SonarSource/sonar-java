package checks.spring;

import java.util.Random;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static java.lang.System.lineSeparator;

public class BeanMethodOfNonProxiedSingletonInvocationCheckSample {
  @Configuration(proxyBeanMethods = false)
  static class NonCompliantConfiguration {
    @Bean
    public SimpleBean simpleBean() {
      return new SimpleBean();
    }

    @Bean
    public CompositeBean compositeBean() {
      return new CompositeBean(simpleBean()); // Noncompliant [[sc=32;ec=44]] {{Replace this bean method invocation with a dependency injection.}}
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class CompliantConfiguration {
    @Bean
    public SimpleBean simpleBean() {
      return new SimpleBean();
    }

    @Bean
    @Scope("Prototype")
    public PrototypeBean prototypeBean() {
      return new PrototypeBean();
    }

    @Bean
    public CompositeBean compositeBean(SimpleBean simpleBean) { // Compliant, the simpleBean is injected in the context and used by every compositeBean
      return new CompositeBean(simpleBean);
    }

    @Bean
    public CompositeBean compositeBeanWithPrototypeDependency() {
      return new CompositeBean(prototypeBean()); // Compliant, beans with a prototype scope are not singletons (ie a new instance is created on each call)
    }
    @Bean
    public NamedBean namedBean() {
      return new NamedBean(lineSeparator());
    }

    public NamedBean anotherNamedBean() {
      return new NamedBean(getAString());
    }
  }

  @Configuration(proxyBeanMethods = true)
  static class ProxyBeanMethodsEnabledExplicitly {
    @Bean
    public SimpleBean simpleBean() {
      return new SimpleBean();
    }

    @Bean
    public CompositeBean compositeBean() {
      return new CompositeBean(simpleBean()); // Compliant, call will be proxied and the singleton instance will be returned
    }
  }

  @Configuration(value = "nothingToSeeHere")
  static class ProxyBeanMethodsEnabledImplicitly {
    @Bean
    public SimpleBean simpleBean() {
      return new SimpleBean();
    }

    @Bean
    public CompositeBean compositeBean() {
      return new CompositeBean(simpleBean()); // Compliant, call will be proxied and the singleton instance will be returned
    }
  }


  static class SimpleBean {
    // ...
  }

  static class CompositeBean {
    public CompositeBean(SimpleBean simpleBean) {
      // ...
    }
  }
  static class PrototypeBean extends SimpleBean {
    // ...
  }

  static class NamedBean {
    private String name;

    public NamedBean(String name) {
      this.name = name;
    }
  }

  @Component
  abstract class AnnotatedClassWithoutAConfigurationAnnotation {
    @Bean
    public SimpleBean simpleBean() {
      return new SimpleBean();
    }

    @Bean
    public CompositeBean compositeBean() {
      return new CompositeBean(simpleBean()); // Compliant because this is not annotated with Configuration
    }
  }


  private static String getAString() {
    return (new Random(42)).nextBoolean() ? "Nothing" : "Something";
  }
}
