package checks.spring;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class AvoidQualifierOnBeanMethodsCheck {

  @Configuration
  class Configuration1 {
    @Bean
    @Qualifier("foo") // Noncompliant, [[sc=5;ec=22]] {{Remove this redundant "@Qualifier" annotation}}
    public String foo() {
      return "foo";
    }

    @Bean
    @Qualifier(value = "bar") // Noncompliant, [[sc=5;ec=30]] {{Remove this redundant "@Qualifier" annotation}}
    public String bar() {
      return "bar";
    }

    @Bean("foobar")      // Compliant
    public String foobar() {
      return "foobar";
    }
  }
}
