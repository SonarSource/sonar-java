import java.lang.reflect.Method;

class A {
  void foo() {
    Method m;
    Class<T> c;
    m.isAnnotationPresent(Override.class); //NonCompliant
    c.isAnnotationPresent(Override.class); //NonCompliant
    m.isAnnotationPresent(Deprecated.class); //Compliant, runtime retention
    m.isAnnotationPresent(bar()); //Compliant, we can't know what the bar method is returning.
    m.isAnnotationPresent(UnknownAnnotation.class); // Compliant, unable to retrieve annotation
  }
  
  Class<? extends Annotation> bar() {
    return null;
  }
}