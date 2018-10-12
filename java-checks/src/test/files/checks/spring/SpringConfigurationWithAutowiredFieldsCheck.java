package src.test.files.checks.spring;

import javax.inject.Inject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class Bar { }

class Foo {
  private final Bar bar;
  public Foo(Bar bar) { this.bar = bar; }
}

@Configuration
class A {

  @Autowired private Bar singleUsage; // Noncompliant [[sc=26;ec=37]] {{Inject this field value directly into "method", the only method that uses it.}}
  @Inject private Bar jsr330; // Noncompliant [[sc=23;ec=29]] {{Inject this field value directly into "jsr330", the only method that uses it.}}
  @Autowired private Bar multipleUsage;
  @Autowired private Bar notUsedInBeanMethod;
  @Autowired private Bar notUsed;
  private Bar notAutowired;

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

public class StaticConfig {
  @Configuration
  public static class InnerStaticConfig {
    @Autowired private Bar x; // Noncompliant

    @Bean
    public Foo method() {
      return new Foo(this.x);
    }
  }
}
