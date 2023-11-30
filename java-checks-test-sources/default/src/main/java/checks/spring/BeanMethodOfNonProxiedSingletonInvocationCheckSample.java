package checks.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

public class BeanMethodOfNonProxiedSingletonInvocationCheckSample {
  static class SingletonBean {
    // ...
  }
  @Scope("Prototype")
  static class PrototypeBean {
    public PrototypeBean(SingletonBean singletonBean) {
      // ...
    }
  }
  @Configuration(proxyBeanMethods = false)
  static class NonCompliantConfiguration {
    @Bean
    public SingletonBean singletonBean() {
      return new SingletonBean();
    }

    @Bean
    public PrototypeBean prototypeBean() {
      return new PrototypeBean(singletonBean()); // Noncompliant, the singletonBean is created every time a prototypeBean is created
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class CompliantConfiguration {
    @Bean
    public SingletonBean singletonBean() {
      return new SingletonBean();
    }
    @Bean
    public PrototypeBean prototypeBean(SingletonBean singletonBean) { // Compliant, the singletonBean is injected in the context and used by every prototypeBean
      return new PrototypeBean(singletonBean);
    }
  }
}
