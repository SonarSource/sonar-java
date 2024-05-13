package checks.spring;

import javax.inject.Inject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class SpringConfigurationWithAutowiredFieldsCheckSample {
  class Bar { }

  class Foo {
    private final Bar bar;
    public Foo(Bar bar) { this.bar = bar; }
  }

  @Configuration
  class A {
    @Autowired private Bar singleUsage; // Noncompliant {{Inject this field value directly into "method", the only method that uses it.}}
//                         ^^^^^^^^^^^
    @Inject private Bar jsr330; // Noncompliant {{Inject this field value directly into "jsr330", the only method that uses it.}}
//                      ^^^^^^
    @Autowired private Bar multipleUsage;
    @Autowired private Bar notUsedInBeanMethod;
    @Autowired private Bar notUsed;
    private Bar notAutowired;
    @Autowired(required=false) private Bar withInitializer = new Bar(); // Compliant, use to define a default value if the bean is not resolved
    @Autowired(required=false) private Bar withoutInitializer; // Noncompliant
    // Default value for required is true.
    // When set to true, Spring will throw an exception if Bean cannot be resolved, still makes sense to report an issue.
    @Autowired(required=true) private Bar withInitializerRequiredTrue = new Bar(); // Noncompliant
    @Autowired private Bar withInitializerRequiredDefault = new Bar(); // Noncompliant

    @Bean
    public Foo method() {
      return new Foo(this.singleUsage);
    }

    @Bean
    public Foo jsr330() {
      return new Foo(this.jsr330);
    }

    @Bean
    public Foo method2() {
      return new Foo(this.multipleUsage);
    }

    @Bean
    public Foo method3() {
      return new Foo(this.multipleUsage);
    }

    public Foo method4() {
      return new Foo(this.notUsedInBeanMethod);
    }

    @Bean
    public Foo method5() {
      return new Foo(this.notAutowired);
    }

    @Bean
    public Foo method6() {
      return new Foo(this.withInitializerRequiredTrue);
    }

    @Bean
    public Foo method7() {
      return new Foo(this.withInitializer);
    }

    @Bean
    public Foo method8() {
      return new Foo(this.withInitializerRequiredDefault);
    }

    @Bean
    public Foo method9() {
      return new Foo(this.withoutInitializer);
    }
  }

  @Configuration
  class B {
    @Autowired private Bar multipleUsage;
    @Bean
    public Foo method() {
      return indirectMethod();
    }
    private Foo indirectMethod() {
      return new Foo(this.multipleUsage);
    }
    @Bean Foo method2() {
      return new Foo(this.multipleUsage);
    }
  }

  @Configuration
  class FalseNegative {

    private Bar bar; // FN

    @Autowired
    public void setBar(Bar bar) {
      this.bar = bar;
    }

    @Bean
    public Foo method() {
      return new Foo(this.bar);
    }
  }

  @Configuration
  class Ok {

    @Bean
    public Foo method(Bar bar) {
      return new Foo(bar);
    }
  }

  @Configuration
  public class NestedConfig {

    @Configuration
    public class InnerConfig {
      @Autowired private Bar x; // Noncompliant
      @Bean
      public Foo method() {
        return new Foo(this.x);
      }
    }
  }

  static class FooStatic {
    public FooStatic(Bar bar) {
    }
  }

  @Configuration
  public static class InnerStaticConfig {
    @Autowired
    private Bar x; // Noncompliant

    @Bean
    public FooStatic method() {
      return new FooStatic(this.x);
    }
  }
}



