package checks;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import static java.lang.annotation.RetentionPolicy.SOURCE;

class ReflectionOnNonRuntimeAnnotationCheckSample {
  private Class<? extends Annotation> annotation;
  void foo(Method m, Class<?> c) {
    m.isAnnotationPresent(Override.class); // Noncompliant [[sc=27;ec=41]] {{"@Override" is not available at runtime and cannot be seen with reflection.}}
    c.isAnnotationPresent(Override.class); // Noncompliant {{"@Override" is not available at runtime and cannot be seen with reflection.}}
    m.isAnnotationPresent(Deprecated.class); //Compliant, runtime retention
    m.isAnnotationPresent(bar());           //Compliant, we can't know what the bar method is returning.
    m.isAnnotationPresent(this.annotation); //Compliant, we can't know what the field annotation is returning.
  }
  
  Class<? extends Annotation> bar() {
    return null;
  }

  @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface Expose1 {}
  @java.lang.annotation.Retention(RetentionPolicy.CLASS)
  @Target(ElementType.METHOD)
  public @interface Expose2 {}
  @java.lang.annotation.Retention(SOURCE)
  @Target(ElementType.METHOD)
  public @interface Expose3 {}
  @Target(ElementType.METHOD)
  public @interface Expose4 {}
  private void addMethod(Class<?> c) {
    boolean annotation = c.isAnnotationPresent(Expose1.class); //Compliant
    boolean annotation2 = c.isAnnotationPresent(Expose2.class); // Noncompliant {{"@Expose2" is not available at runtime and cannot be seen with reflection.}}
    boolean annotation3 = c.isAnnotationPresent(Expose3.class); // Noncompliant {{"@Expose3" is not available at runtime and cannot be seen with reflection.}}
    boolean annotation4 = c.isAnnotationPresent(Expose4.class); // Noncompliant {{"@Expose4" is not available at runtime and cannot be seen with reflection.}}
  }
}
