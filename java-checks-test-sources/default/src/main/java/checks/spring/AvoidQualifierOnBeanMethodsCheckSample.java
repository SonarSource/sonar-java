package checks.spring;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

public class AvoidQualifierOnBeanMethodsCheckSample {

  private static final String FOO = "foo";
  private static final String CAPITALIZED_FOO = "Foo";

  @Configuration
  class Configuration1 {
    @Bean
    @Qualifier("foo") // Noncompliant [[quickfixes=qf1]] {{Remove this redundant "@Qualifier" annotation and rely on the @Bean method.}}
//  ^^^^^^^^^^^^^^^^^
    // fix@qf1 {{Remove "@Qualifier"}}
    // edit@qf1 [[sc=5;ec=22]] {{}}
    public String foo() {
      return "foo";
    }

    @Bean
    @Qualifier(value = "bar") // Noncompliant
    public String bar() {
      return "bar";
    }

    @Bean // Compliant
    public String foobar() {
      return "foobar";
    }
  }

  @Component
  class Component1 {
    @Bean("foo")
    @Qualifier(CAPITALIZED_FOO) // Noncompliant
    public String foo() {
      return "foo";
    }

    @Bean(name = "bar")
    @Qualifier(value = "Bar") // Noncompliant
    public String bar() {
      return "bar";
    }

    @Bean("foobar") // Compliant
    public String foobar() {
      return "foobar";
    }
  }

  class Class1 {
    @Bean("foo")
    @Qualifier(FOO) // Noncompliant
    public String foo() {
      return "foo";
    }

    @Bean(name = "bar")
    @Qualifier // Noncompliant [[quickfixes=qf3]] {{Remove this redundant "@Qualifier" annotation and rely on the @Bean method.}}
//  ^^^^^^^^^^
    // fix@qf3 {{Remove "@Qualifier"}}
    // edit@qf3 [[sc=5;ec=15]] {{}}
    public String bar() {
      return "bar";
    }

    @Bean("foobar") // Compliant
    public String foobar() {
      return "foobar";
    }
  }

}
