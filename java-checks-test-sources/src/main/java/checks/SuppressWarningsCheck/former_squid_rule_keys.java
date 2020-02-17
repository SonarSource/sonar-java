package checks.SuppressWarningsCheck;

class former_squid_rule_keys {

  @SuppressWarnings("squid:S1068")
  private void f1() {
  }

  @SuppressWarnings("java:S1068")
  private void f2() {
  }

  @SuppressWarnings("squid:S115")
  private void f3() {
  }

  @SuppressWarnings("java:S115")
  private void f4() {
  }

  @SuppressWarnings({"squid:S1068", "squid:S115"})
  private void f5() {
  }

  @SuppressWarnings("squid:S123") // Noncompliant
  private void f6() {
  }

  @SuppressWarnings("java:S123") // Noncompliant
  private void f7() {
  }

  @SuppressWarnings("other:S115") // Noncompliant
  private void f8() {
  }

  @SuppressWarnings("S115") // Noncompliant
  private void f9() {
  }

}
