class Fields {
  @lombok.Getter
  class Getter { // WithIssue
    private int foo; // NoIssue
  }

  class Getter2 { // WithIssue
    private int foo; // WithIssue
  }

  @lombok.Setter
  class Setter { // WithIssue
    private int foo; // NoIssue
  }

  class Setter2 { // WithIssue
    private int foo; // WithIssue
  }

  @lombok.Data
  class Data { // WithIssue
    private int foo; // NoIssue
  }

  class Data2 { // WithIssue
    private int foo; // WithIssue
  }

  @lombok.Value
  class Value { // WithIssue
    private int foo; // NoIssue
  }

  class Value2 { // WithIssue
    private int foo; // WithIssue
  }

  @lombok.Builder
  class Builder { // WithIssue
    private int foo; // NoIssue
  }

  class Builder2 { // WithIssue
    private int foo; // WithIssue
  }

  @lombok.ToString
  class ToString { // WithIssue
    private int foo; // NoIssue
  }

  class ToString2 { // WithIssue
    private int foo; // WithIssue
  }

  @lombok.RequiredArgsConstructor
  class RequiredArgsConstructor { // NoIssue
    private int foo; // NoIssue
  }

  class RequiredArgsConstructor2 { // WithIssue
    private int foo; // WithIssue
  }

  @lombok.AllArgsConstructor
  class AllArgsConstructor { // NoIssue
    private int foo; // NoIssue
  }

  class AllArgsConstructor2 { // WithIssue
    private int foo; // WithIssue
  }

  @lombok.NoArgsConstructor
  class NoArgsConstructor { // NoIssue
    private int foo; // NoIssue
  }

  class NoArgsConstructor2 { // WithIssue
    private int foo; // WithIssue
  }

  @lombok.EqualsAndHashCode
  class EqualsAndHashCode { // WithIssue
    private int foo; // NoIssue
  }

  class EqualsAndHashCode2 { // WithIssue
    private int foo; // WithIssue
  }
}

class EqualsNotOverriddenInSubclass {
  class A {
    String s1;
  }

  class B extends A { // NoIssue
    String s2;
  }

  @lombok.EqualsAndHashCode
  class B1 extends A { // NoIssue
    String s2;
  }

  @lombok.Data
  class B2 extends A { // NoIssue
    String s2;
  }

  @lombok.Value
  class B3 extends A { // NoIssue
    String s2;
  }
}

public class EqualsNotOverridenWithCompareToCheck implements Comparable {

  class A implements Comparable {
    public int compareTo(Object o) { // WithIssue
      return 0;
    }
  }

  @lombok.EqualsAndHashCode
  class A1 implements Comparable<A> {
    @Override
    public int compareTo(A o) { // NoIssue
      return 0;
    }
  }

  class A2 implements Comparable<A> {
    @Override
    public int compareTo(A o) { // WithIssue
      return 0;
    }
  }

  @lombok.Data
  class B1 implements Comparable<A> {
    @Override
    public int compareTo(A o) { // NoIssue
      return 0;
    }
  }

  class B2 implements Comparable<A> {
    @Override
    public int compareTo(A o) { // WithIssue
      return 0;
    }
  }

  @lombok.Value
  class C1 implements Comparable<A> {
    @Override
    public int compareTo(A o) { // NoIssue
      return 0;
    }
  }

  class C2 implements Comparable<A> {
    @Override
    public int compareTo(A o) {  // WithIssue
      return 0;
    }
  }
}

static class UtilityClass {
  private UtilityClass() {}

  static class A { // WithIssue
    public static void foo() {
    }
  }

  @lombok.experimental.UtilityClass
  static class B { // NoIssue
    public static void foo() {
    }
  }
}
