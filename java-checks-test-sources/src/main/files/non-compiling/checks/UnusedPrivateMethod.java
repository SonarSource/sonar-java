package checks;

class KillTheNoiseUnresolvedMethodCall {

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
}
