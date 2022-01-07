import java.util.*;

class A {
  @SomethingUnknown
  public void resourceAnnotatedMethod1(List<Object> list) { // Compliant - Unknown annotation, could be Spring, we do not report anything
    for (Object o : list) {
      o.toString();
    }
  }

  @SomethingUnknown
  public void resourceAnnotatedMethod2(Collection<Object> list) { // Compliant - Unknown annotation, could be Spring, we do not report anything
    for (Object o : list) {
      o.toString();
    }
  }
}
