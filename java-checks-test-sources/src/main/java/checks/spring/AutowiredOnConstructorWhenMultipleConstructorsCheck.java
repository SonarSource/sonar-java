package checks.spring;

import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

public class AutowiredOnConstructorWhenMultipleConstructorsCheck { // Compliant

  public AutowiredOnConstructorWhenMultipleConstructorsCheck() {
  }

  public AutowiredOnConstructorWhenMultipleConstructorsCheck(int i) {
  }

  public AutowiredOnConstructorWhenMultipleConstructorsCheck(String s) {
  }

  @Component
  class SpringComponent { // Noncompliant [[sc=9;ec=24]] {{Add @Autowired to one of the constructors.}}

    public SpringComponent() {
    }

    public SpringComponent(int i) {
    }

    public SpringComponent(String s) {
    }
  }

  @Component
  class CompliantSpringComponent { // Compliant

    @Autowired
    public CompliantSpringComponent() {
    }

    public CompliantSpringComponent(int i) {
    }

    public CompliantSpringComponent(String s) {
    }
  }

  @Component
  class ComponentWithOtherAnnotations { // Noncompliant

    public ComponentWithOtherAnnotations() {
    }

    public ComponentWithOtherAnnotations(int i) {
    }

    @Deprecated
    public ComponentWithOtherAnnotations(String s) {
    }
  }

  @Component
  class ComponentWithOneConstructorWithAutowired { // Compliant

    public ComponentWithOneConstructorWithAutowired() {
    }
  }

  @Nullable
  class ClassWithSomeOtherAnnotation { // Compliant

    public ClassWithSomeOtherAnnotation() {
    }

    public ClassWithSomeOtherAnnotation(int i) {
    }

    public ClassWithSomeOtherAnnotation(String s) {
    }
  }

  @Nullable
  @Component
  class ClassWithTwoAnnotations { // Noncompliant

    public ClassWithTwoAnnotations() {
    }

    public ClassWithTwoAnnotations(int i) {
    }

    public ClassWithTwoAnnotations(String s) {
    }
  }

}
