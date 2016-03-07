import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import static java.lang.annotation.RetentionPolicy.SOURCE;

class A {
  void foo() {
    Method m;
    Class<T> c;
    m.isAnnotationPresent(Override.class); // Noncompliant [[sc=27;ec=41]] {{"@Override" is not available at runtime and cannot be seen with reflection.}}
    c.isAnnotationPresent(Override.class); // Noncompliant {{"@Override" is not available at runtime and cannot be seen with reflection.}}
    m.isAnnotationPresent(Deprecated.class); //Compliant, runtime retention
    m.isAnnotationPresent(bar()); //Compliant, we can't know what the bar method is returning.
    m.isAnnotationPresent(UnknownAnnotation.class); // Compliant, unable to retrieve annotation
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
  private void addMethod(Class<T> c) {
    Expose annotation = c.isAnnotationPresent(Expose1.class); //Compliant
    Expose annotation2 = c.isAnnotationPresent(Expose2.class); // Noncompliant {{"@Expose2" is not available at runtime and cannot be seen with reflection.}}
    Expose annotation3 = c.isAnnotationPresent(Expose3.class); // Noncompliant {{"@Expose3" is not available at runtime and cannot be seen with reflection.}}
    Expose annotation4 = c.isAnnotationPresent(Expose4.class); // Noncompliant {{"@Expose4" is not available at runtime and cannot be seen with reflection.}}
  }
}
