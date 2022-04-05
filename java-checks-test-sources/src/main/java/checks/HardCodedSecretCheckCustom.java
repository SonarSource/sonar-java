package checks;

class HardCodedSecretCheckCustom {
  private void test() {
    String variable2 = "login=a&secret=abcd"; // Compliant
    String variable5 = "login=a&marmalade=abcd"; // Noncompliant [[sc=24;ec=48]] {{'marmalade' detected in this expression, review this potentially hard-coded secret.}}
  }
}
