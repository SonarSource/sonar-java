package checks;

class KillTheNoiseUnresolvedMethodCall {

  private void init(@UknownAnnotation Object object) {} // Noncompliant
  private void init2(@somepackage.Observes Object object) {} // Noncompliant
  
  static class A {
    private A(int i) {}  // Compliant - unresolved constructor invocation
  }

  void foo(Object o, java.util.List<Object> objects) {
    unresolvedMethod(unknown); // unresolved

    A a;
    a = new A(unknown); // unresolved
    a = new org.sonar.java.checks.targets.KillTheNoiseUnresolvedMethodCall.A(o); // unresolved
    A[] as = new A[0];

    new Unknown<String>(o); // unresolved

    objects.stream().forEach(this::unresolvedMethodRef); // unresolved
    objects.stream().forEach(this::methodRef); // resolved
  }

  private void unresolvedMethod(int i) {} // Compliant - method with the same name not resolved
  private void unresolvedMethodRef(int i) {} // Compliant - method ref with the same name not resolved
  private void methodRef(Object o) {} // Compliant

  static class Member {
    private Member(String firstName, String lastName, String memberID) { // Compliant
    }

    public static LastNameBuilder member(String firstName) {
      return lastName -> memberID -> new Member(firstName, lastName, memberID); // constructor not resolved
    }

    @FunctionalInterface
    public interface LastNameBuilder {
      MemberIDBuilder lastName(String lastName);
    }

    @FunctionalInterface
    public interface MemberIDBuilder {
      Member memberID(String memberID);
    }
  }

  private void mOverloaded(Unknown1 s) { // Compliant
  }

  private void mOverloaded(Unknown2 s) { // Compliant
  }

  void callmOverloaded(Unknown1 u1, Unknown2 u2) {
    mOverloaded(u1);
    mOverloaded(u2); // Incorrectly resolved to the other overload
  }

  private void mOverloaded2(Object s) { // Compliant
  }

  private void mOverloaded2(Unknown2 s) { // Compliant
  }
  void callmOverloaded(java.util.List<Unknown2> objects) {
    objects.stream().forEach(this::mOverloaded2);
    objects.stream().forEach(this::mOverloaded2);
  }

  class OverLoadConstructor {
    private OverLoadConstructor(Unknown1 s) {
      System.out.println();
    }

    private OverLoadConstructor(Unknown2 s) {
      System.out.println();
    }

    void callConstructor(Unknown1 u1, Unknown2 u2) {
      new OverLoadConstructor(u1);
      new OverLoadConstructor(u2);
    }
  }

  private void doSomething(byte i) {
  }

  private void doSomething(int i) {
  }

  void callDoSomething() {
    doSomething(someInt.UNKNOWN);
    doSomething(someByte.UNKNOWN);
  }
}
