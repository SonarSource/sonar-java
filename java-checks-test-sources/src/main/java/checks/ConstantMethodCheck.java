package checks;

import javax.validation.constraints.NotNull;

public final class ConstantMethodCheck implements Comparable<ConstantMethodCheck> {

  int foo() {
    return 1; // Noncompliant [[sc=12;ec=13]] {{Remove this method and declare a constant for this value.}}
  }

  boolean baz() {
    return true; // Noncompliant [[sc=12;ec=16]] {{Remove this method and declare a constant for this value.}}
  }

  String bar() {
    return ""; // Noncompliant [[sc=12;ec=14]] {{Remove this method and declare a constant for this value.}}
  }
  char qix() {
    return 'c'; // Noncompliant [[sc=12;ec=15]] {{Remove this method and declare a constant for this value.}}
  }
  Object lum() {
    return new Object(); // Compliant
  }
  int gul() {
    System.out.println("foo");
    return 1;
  }

  void bah(){
    return;
  }
  void tol(){
    System.out.println("");
  }

  @Override
  public String toString() {
    return "";  // compliant, this method is an override
  }

  // removed @Override annotation
  public int compareTo(@NotNull ConstantMethodCheck o) {
    return 0; // Compliant - method is an override
  }
  long gro() {
    return 1L; // Noncompliant [[sc=12;ec=14]] {{Remove this method and declare a constant for this value.}}
  }

  @MyAnnotation
  long annotatedMethod() {
    return 1L; // Compliant
  }

  @interface MyAnnotation {}

  interface Interface {
    String methodWithoutBody();

    default String methodReturningConstant() {
      return "";
    }
  }

  static class NoFinalClass {
    final long finalMethod() {
      return 1L; // Noncompliant
    }

    long noFinalMethod() {
      return 1L; // Compliant
    }

    private long nonFinalButPrivateMethod() {
      return 1L; // Noncompliant
    }
  }

  record Record(int value) {
    final long finalMethod() {
      return 1L; // Noncompliant
    }
    String noFinalMethod() {
      // Noncompliant@+1
      return """
          text block
        """;
    }
  }

  abstract class AbstractClass {
    abstract void abstractMethod(); // Compliant

    long noFinalMethod() {
      return 1L; // Compliant
    }
  }
}

